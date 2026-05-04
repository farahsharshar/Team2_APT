package com.Team2_CDE_master.ProjectServer.crdt;

import java.util.ArrayList;
import java.util.HashMap;

public class BlockCRDT {
    public ArrayList<Block> allBlocks;

    private final HashMap<String, Block> blockMap = new HashMap<>();

    public BlockCRDT() {
        allBlocks = new ArrayList<>();
    }

    private String key(BlockID id) {
        return id.siteId + "_" + id.counter;
    }

    public void addBlock(Block newBlock) {
        int parentPos = -1;
        if (newBlock.getParentId() != null) {
            for (int i = 0; i < allBlocks.size(); i++) {
                if (allBlocks.get(i).getMyId().isSameAs(newBlock.getParentId())) {
                    parentPos = i;
                    break;
                }
            }
        }
        int insertAt = parentPos + 1;
        while (insertAt < allBlocks.size()) {
            Block current = allBlocks.get(insertAt);
            boolean sameParent = false;
            if (newBlock.getParentId() == null && current.getParentId() == null) {
                sameParent = true;
            } else if (newBlock.getParentId() != null && current.getParentId() != null) {
                sameParent = current.getParentId().isSameAs(newBlock.getParentId());
            }

            if (sameParent) {
                if (current.getMyId().siteId > newBlock.getMyId().siteId) {
                    insertAt++;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        allBlocks.add(insertAt, newBlock);
        blockMap.put(key(newBlock.getMyId()), newBlock);
    }

    public void deleteBlock(BlockID targetId) {
        Block block = findBlock(targetId);
        if (block != null) {
            block.markDeleted();
        }
    }

    public void splitBlock(BlockID targetId, int splitIndex, BlockID newBlockId) {
        Block original = findBlock(targetId);
        if (original == null || original.checkDeleted()) {
            System.out.println("splitBlock: block not found or already deleted — " + targetId);
            return;
        }
        ArrayList<CharNode> visibleNodes = new ArrayList<>();
        for (CharNode node : original.getContent().allNodes) {
            if (!node.checkDeleted()) {
                visibleNodes.add(node);
            }
        }
        if (splitIndex <= 0 || splitIndex >= visibleNodes.size()) {
            System.out.println("splitBlock: splitIndex " + splitIndex + " is out of range for block " + targetId);
            return;
        }
        Block newBlock = new Block(newBlockId, targetId);
        for (int i = splitIndex; i < visibleNodes.size(); i++) {
            CharNode node = visibleNodes.get(i);
            node.markDeleted();
            CharID newParentId = (i == splitIndex) ? null : visibleNodes.get(i - 1).getMyId();
            CharNode copy = new CharNode(node.getMyId(), newParentId, node.getMyChar());
            copy.setBold(node.checkBold());
            copy.setItalic(node.checkItalic());
            newBlock.getContent().addChar(copy);
        }
        addBlock(newBlock);
    }

    public void mergeBlocks(BlockID firstId, BlockID secondId) {
        Block first = findBlock(firstId);
        Block second = findBlock(secondId);

        if (first == null || second == null) {
            System.out.println("mergeBlocks: one or both blocks not found");
            return;
        }
        if (first.checkDeleted() || second.checkDeleted()) {
            System.out.println("mergeBlocks: one or both blocks already deleted");
            return;
        }
        CharNode lastInFirst = null;
        for (CharNode node : first.getContent().allNodes) {
            if (!node.checkDeleted()) {
                lastInFirst = node;
            }
        }
        ArrayList<CharNode> toMove = new ArrayList<>();
        for (CharNode node : second.getContent().allNodes) {
            if (!node.checkDeleted()) {
                toMove.add(node);
            }
        }
        for (int i = 0; i < toMove.size(); i++) {
            CharNode original = toMove.get(i);
            CharID newParentId = (i == 0)
                    ? (lastInFirst != null ? lastInFirst.getMyId() : null)
                    : toMove.get(i - 1).getMyId();

            CharNode copy = new CharNode(original.getMyId(), newParentId, original.getMyChar());
            copy.setBold(original.checkBold());
            copy.setItalic(original.checkItalic());
            first.getContent().addChar(copy);
        }
        second.markDeleted();
    }

    public Block findBlock(BlockID targetId) {
        return blockMap.get(key(targetId));
    }

    public String getFullText() {
        StringBuilder sb = new StringBuilder();
        boolean firstVisible = true;
        for (Block block : allBlocks) {
            if (!block.checkDeleted()) {
                if (!firstVisible) sb.append("\n");
                sb.append(block.getText());
                firstVisible = false;
            }
        }
        return sb.toString();
    }

    public int getBlockCount() {
        int count = 0;
        for (Block block : allBlocks) {
            if (!block.checkDeleted()) {
                count++;
            }
        }
        return count;
    }

    public void printAll() {
        System.out.println("=== BlockCRDT (all blocks including deleted) ===");
        for (Block block : allBlocks) {
            System.out.println("  " + block.toString());
        }
        System.out.println("Full visible text:\n\"" + getFullText() + "\"");
        System.out.println("================================================");
    }
}
