package com.Team2_CDE_master.ProjectServer.crdt;

// this class represents a block-level operation
// it wraps the type and data of any block operation so we can send it over the network in Phase 2
//
// supported types:
//   "insert_block"  — add a new block to the document
//   "delete_block"  — tombstone an existing block
//   "split_block"   — split one block into two at a character position
//   "merge_blocks"  — merge two consecutive blocks into one
public class BlockOperation {

    String type;            // one of the four type strings above

    // used for insert_block
    Block blockToAdd;

    // used for delete_block
    BlockID targetId;

    // used for split_block
    BlockID splitTargetId;
    int splitIndex;         // visible character index where the split happens
    BlockID newBlockId;     // id for the newly created second half

    // used for merge_blocks
    BlockID firstId;
    BlockID secondId;

    // ---- factory constructors — one for each operation type ----

    public static BlockOperation insertBlock(Block block) {
        BlockOperation op = new BlockOperation();
        op.type = "insert_block";
        op.blockToAdd = block;
        return op;
    }

    public static BlockOperation deleteBlock(BlockID targetId) {
        BlockOperation op = new BlockOperation();
        op.type = "delete_block";
        op.targetId = targetId;
        return op;
    }

    public static BlockOperation splitBlock(BlockID splitTargetId, int splitIndex, BlockID newBlockId) {
        BlockOperation op = new BlockOperation();
        op.type = "split_block";
        op.splitTargetId = splitTargetId;
        op.splitIndex = splitIndex;
        op.newBlockId = newBlockId;
        return op;
    }

    public static BlockOperation mergeBlocks(BlockID firstId, BlockID secondId) {
        BlockOperation op = new BlockOperation();
        op.type = "merge_blocks";
        op.firstId = firstId;
        op.secondId = secondId;
        return op;
    }

    private BlockOperation() {}

    // apply this operation to a BlockCRDT
    public void apply(BlockCRDT blockCRDT) {
        switch (type) {
            case "insert_block":
                blockCRDT.addBlock(blockToAdd);
                break;
            case "delete_block":
                blockCRDT.deleteBlock(targetId);
                break;
            case "split_block":
                blockCRDT.splitBlock(splitTargetId, splitIndex, newBlockId);
                break;
            case "merge_blocks":
                blockCRDT.mergeBlocks(firstId, secondId);
                break;
            default:
                System.out.println("BlockOperation.apply: unknown type — " + type);
        }
    }

    public String getType() { return type; }

    public String toString() {
        switch (type) {
            case "insert_block":  return "BlockOp[insert, block=" + blockToAdd.getMyId() + "]";
            case "delete_block":  return "BlockOp[delete, target=" + targetId + "]";
            case "split_block":   return "BlockOp[split, target=" + splitTargetId + ", at=" + splitIndex + ", newBlock=" + newBlockId + "]";
            case "merge_blocks":  return "BlockOp[merge, first=" + firstId + ", second=" + secondId + "]";
            default:              return "BlockOp[unknown type=" + type + "]";
        }
    }
}
