package com.Team2_CDE_master.ProjectServer.persistence;

import com.Team2_CDE_master.ProjectServer.crdt.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Handles saving and loading the CRDT document structure to/from the database.
// This is the main class that bridges between BlockCRDT (in-memory) and the DB tables.
@Service
public class DocumentPersistenceService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private CharNodeRepository charNodeRepository;

    // -------------------------------------------------------------------------
    // SAVE — serializes a full BlockCRDT to the database
    // If the document already exists, it is overwritten (all rows are replaced).
    // Returns the DocumentEntity that was saved.
    // -------------------------------------------------------------------------
    @Transactional
    public DocumentEntity saveDocument(BlockCRDT doc, String docId, String docName) {
        // 1. Upsert the document metadata row
        DocumentEntity docEntity = documentRepository.findById(docId)
                .orElse(new DocumentEntity(docId, docName));
        docEntity.setName(docName);
        docEntity.setUpdatedAt(LocalDateTime.now());
        if (docEntity.getCreatedAt() == null) {
            docEntity.setCreatedAt(LocalDateTime.now());
        }
        documentRepository.save(docEntity);

        // 2. Delete existing block and char data so we start fresh
        //    (We do a full replace on every save — simple but reliable)
        charNodeRepository.deleteByDocumentId(docId);
        blockRepository.deleteByDocumentId(docId);

        // 3. Walk every block and every char node and write them to the DB
        List<BlockEntity> blockRows = new ArrayList<>();
        List<CharNodeEntity> charRows = new ArrayList<>();

        int blockOrder = 0;
        for (Block block : doc.allBlocks) {
            // Build the block row
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

            // Build a row for each char node in this block (including tombstones)
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

        // Batch insert for efficiency
        blockRepository.saveAll(blockRows);
        charNodeRepository.saveAll(charRows);

        System.out.println("[Persistence] Saved '" + docId + "' — "
                + blockOrder + " blocks, " + charRows.size() + " char nodes");
        return docEntity;
    }

    // -------------------------------------------------------------------------
    // LOAD — reconstructs a BlockCRDT from the database
    // Returns null if the document ID is not found.
    // After calling this, you can seed the in-memory session with the result.
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public BlockCRDT loadDocument(String docId) {
        if (!documentRepository.existsById(docId)) {
            System.out.println("[Persistence] Document '" + docId + "' not in DB");
            return null;
        }

        BlockCRDT doc = new BlockCRDT();

        // Load blocks in the order they were saved (ordering column = list position)
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

            // Load this block's char nodes in their saved order
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

                // The charValue was stored as a single-character String
                char ch = ce.getCharValue() != null && !ce.getCharValue().isEmpty()
                        ? ce.getCharValue().charAt(0) : ' ';

                CharNode node = new CharNode(charId, parentCharId, ch);
                if (ce.isDeleted()) node.markDeleted();
                node.setBold(ce.isBold());
                node.setItalic(ce.isItalic());

                // addChar re-applies the CRDT ordering logic.
                // Loading in saved order (parents before children) reproduces the same layout.
                block.getContent().addChar(node);
            }

            // addBlock re-applies block ordering — same reasoning as above
            doc.addBlock(block);
        }

        System.out.println("[Persistence] Loaded '" + docId + "' — "
                + doc.allBlocks.size() + " blocks");
        return doc;
    }

    // -------------------------------------------------------------------------
    // LIST — returns metadata for every saved document
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<DocumentEntity> listDocuments() {
        return documentRepository.findAll();
    }

    // -------------------------------------------------------------------------
    // DELETE — removes a document and all its blocks/chars from the DB
    // -------------------------------------------------------------------------
    @Transactional
    public void deleteDocument(String docId) {
        charNodeRepository.deleteByDocumentId(docId);
        blockRepository.deleteByDocumentId(docId);
        documentRepository.deleteById(docId);
        System.out.println("[Persistence] Deleted document '" + docId + "'");
    }

    // -------------------------------------------------------------------------
    // EXISTS — quick check used by the REST controller
    // -------------------------------------------------------------------------
    public boolean documentExists(String docId) {
        return documentRepository.existsById(docId);
    }
}
