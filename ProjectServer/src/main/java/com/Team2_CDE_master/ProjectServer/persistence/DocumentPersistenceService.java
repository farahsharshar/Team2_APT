package com.Team2_CDE_master.ProjectServer.persistence;

import com.Team2_CDE_master.ProjectServer.crdt.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentPersistenceService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private CharNodeRepository charNodeRepository;

    @Transactional
    public DocumentEntity saveDocument(BlockCRDT doc, String docId, String docName) {
        DocumentEntity docEntity = documentRepository.findById(docId)
                .orElse(new DocumentEntity(docId, docName));
        docEntity.setName(docName);
        docEntity.setUpdatedAt(LocalDateTime.now());
        if (docEntity.getCreatedAt() == null) {
            docEntity.setCreatedAt(LocalDateTime.now());
        }
        documentRepository.save(docEntity);

        charNodeRepository.deleteByDocumentId(docId);
        blockRepository.deleteByDocumentId(docId);

        List<BlockEntity> blockRows = new ArrayList<>();
        List<CharNodeEntity> charRows = new ArrayList<>();

        int blockOrder = 0;
        for (Block block : doc.allBlocks) {
            BlockEntity be = new BlockEntity();
            be.setDocumentId(docId);
            be.setBlockSiteId(block.getMyId().siteId);
            be.setBlockCounter(block.getMyId().counter);
            be.setDeleted(block.checkDeleted());
            be.setOrdering(blockOrder++);

            if (block.getParentId() != null) {
                be.setParentSiteId(block.getParentId().siteId);
                be.setParentCounter(block.getParentId().counter);
            }
            blockRows.add(be);

            int charOrder = 0;
            for (CharNode node : block.getContent().allNodes) {
                CharNodeEntity ce = new CharNodeEntity();
                ce.setDocumentId(docId);
                ce.setBlockSiteId(block.getMyId().siteId);
                ce.setBlockCounter(block.getMyId().counter);
                ce.setCharSiteId(node.getMyId().siteId);
                ce.setCharNum(node.getMyId().myNum);
                ce.setCharValue(String.valueOf(node.getMyChar()));
                ce.setDeleted(node.checkDeleted());
                ce.setBold(node.checkBold());
                ce.setItalic(node.checkItalic());
                ce.setOrdering(charOrder++);

                if (node.getParentId() != null) {
                    ce.setParentSiteId(node.getParentId().siteId);
                    ce.setParentNum(node.getParentId().myNum);
                }
                charRows.add(ce);
            }
        }

        blockRepository.saveAll(blockRows);
        charNodeRepository.saveAll(charRows);

        System.out.println("[Persistence] Saved '" + docId + "' — "
                + blockOrder + " blocks, " + charRows.size() + " char nodes");
        return docEntity;
    }

    @Transactional(readOnly = true)
    public BlockCRDT loadDocument(String docId) {
        if (!documentRepository.existsById(docId)) {
            System.out.println("[Persistence] Document '" + docId + "' not in DB");
            return null;
        }

        BlockCRDT doc = new BlockCRDT();

        List<BlockEntity> blockEntities =
                blockRepository.findByDocumentIdOrderByOrdering(docId);

        for (BlockEntity be : blockEntities) {
            BlockID blockId = new BlockID(be.getBlockSiteId(), be.getBlockCounter());

            BlockID parentId = null;
            if (be.getParentSiteId() != null) {
                parentId = new BlockID(be.getParentSiteId(), be.getParentCounter());
            }

            Block block = new Block(blockId, parentId);
            if (be.isDeleted()) block.markDeleted();

            List<CharNodeEntity> charEntities =
                    charNodeRepository
                            .findByDocumentIdAndBlockSiteIdAndBlockCounterOrderByOrdering(
                                    docId, be.getBlockSiteId(), be.getBlockCounter());

            for (CharNodeEntity ce : charEntities) {
                CharID charId = new CharID(ce.getCharSiteId(), ce.getCharNum());

                CharID parentCharId = null;
                if (ce.getParentSiteId() != null) {
                    parentCharId = new CharID(ce.getParentSiteId(), ce.getParentNum());
                }

                char ch = ce.getCharValue() != null && !ce.getCharValue().isEmpty()
                        ? ce.getCharValue().charAt(0) : ' ';

                CharNode node = new CharNode(charId, parentCharId, ch);
                if (ce.isDeleted()) node.markDeleted();
                node.setBold(ce.isBold());
                node.setItalic(ce.isItalic());

                block.getContent().addChar(node);
            }

            doc.addBlock(block);
        }

        System.out.println("[Persistence] Loaded '" + docId + "' — "
                + doc.allBlocks.size() + " blocks");
        return doc;
    }

    @Transactional(readOnly = true)
    public List<DocumentEntity> listDocuments() {
        return documentRepository.findAll();
    }

    @Transactional
    public void deleteDocument(String docId) {
        charNodeRepository.deleteByDocumentId(docId);
        blockRepository.deleteByDocumentId(docId);
        documentRepository.deleteById(docId);
        System.out.println("[Persistence] Deleted document '" + docId + "'");
    }

    public boolean documentExists(String docId) {
        return documentRepository.existsById(docId);
    }
}
