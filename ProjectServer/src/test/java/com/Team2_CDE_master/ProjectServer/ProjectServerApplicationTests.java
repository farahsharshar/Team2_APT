package com.Team2_CDE_master.ProjectServer;

import com.Team2_CDE_master.ProjectServer.crdt.*;
import org.junit.jupiter.api.Test;

public class ProjectServerApplicationTests {

	// ===== Member 1 tests =====

	@Test
	void testMember1Insert() {
		System.out.println("===== Testing Member 1: Character Insert =====\n");

		CharCRDT myDoc = new CharCRDT();

		CharID id1 = new CharID(1, 1);
		CharNode nodeH = new CharNode(id1, null, 'H');
		CharID id2 = new CharID(1, 2);
		CharNode nodeE = new CharNode(id2, id1, 'e');
		CharID id3 = new CharID(1, 3);
		CharNode nodeL1 = new CharNode(id3, id2, 'l');
		CharID id4 = new CharID(1, 4);
		CharNode nodeL2 = new CharNode(id4, id3, 'l');
		CharID id5 = new CharID(1, 5);
		CharNode nodeO = new CharNode(id5, id4, 'o');

		myDoc.addChar(nodeH);
		myDoc.addChar(nodeE);
		myDoc.addChar(nodeL1);
		myDoc.addChar(nodeL2);
		myDoc.addChar(nodeO);

		System.out.println("After user 1 types 'Hello':");
		myDoc.printAll();

		CharID id6 = new CharID(1, 6);
		CharNode nodeExclaim = new CharNode(id6, id5, '!');
		InsertOperation op = new InsertOperation("B1_1", nodeExclaim);
		myDoc.addChar(op.getCharToAdd());

		System.out.println("After adding '!': " + myDoc.getText());
		assert myDoc.getText().equals("Hello!") : "Expected Hello!";
	}

	@Test
	void testMember1ConcurrentInsert() {
		System.out.println("===== Testing Concurrent Insert =====\n");

		CharCRDT doc2 = new CharCRDT();

		CharID userOneFirst = new CharID(1, 1);
		doc2.addChar(new CharNode(userOneFirst, null, 'A'));

		CharID userOneX = new CharID(1, 2);
		CharNode nodeX = new CharNode(userOneX, userOneFirst, 'X');
		CharID userTwoY = new CharID(2, 1);
		CharNode nodeY = new CharNode(userTwoY, userOneFirst, 'Y');

		doc2.addChar(nodeX);
		doc2.addChar(nodeY);

		System.out.println("Expected: AYX");
		System.out.println("Got:      " + doc2.getText());
		assert doc2.getText().equals("AYX") : "Expected AYX";
		doc2.printAll();
	}

	// ===== Member 2 tests =====

	@Test
	void testDelete1() {
		System.out.println("===== Testing Member 2: Delete =====\n");

		CharCRDT doc = new CharCRDT();
		CharID a1 = new CharID(1, 1);
		CharID a2 = new CharID(1, 2);
		CharID a3 = new CharID(1, 3);
		doc.addChar(new CharNode(a1, null, 'H'));
		doc.addChar(new CharNode(a2, a1, 'i'));
		doc.addChar(new CharNode(a3, a2, '!'));
		System.out.println("Before delete: " + doc.getText());
		assert doc.getText().equals("Hi!") : "Expected Hi!";
		DeleteCharOperation delOp = new DeleteCharOperation("B1_1", a2);
		delOp.apply(doc);
		System.out.println("After deleting 'i': " + doc.getText());
		assert doc.getText().equals("H!") : "Expected H!";
		doc.printAll();
	}
	@Test
	void testDelete2() {
		CharCRDT doc = new CharCRDT();
		CharID a1 = new CharID(1, 1);
		CharID a2 = new CharID(1, 2);
		CharID a3 = new CharID(1, 3);
		CharID a4 = new CharID(1, 4);
		CharID a5 = new CharID(1, 5);
		doc.addChar(new CharNode(a1, null, 'f'));
		doc.addChar(new CharNode(a2, a1, 'a'));
		doc.addChar(new CharNode(a3, a2, 'r'));
		doc.addChar(new CharNode(a4, a3, 'a'));
		doc.addChar(new CharNode(a5, a4, 'h'));
		System.out.println("Before delete: " + doc.getText());
		assert doc.getText().equals("farah") : "Expected farah";
		DeleteCharOperation delOp = new DeleteCharOperation("B1_1", a1);
		delOp.apply(doc);
		System.out.println("After deleting 'f': " + doc.getText());
		assert doc.getText().equals("arah") : "Expected arah";
		doc.printAll();
	}

	@Test
	void testReplace() {
		System.out.println("===== Testing Member 2: Replace =====\n");

		CharCRDT doc = new CharCRDT();

		CharID b1 = new CharID(1, 1);
		CharID b2 = new CharID(1, 2);
		CharID b3 = new CharID(1, 3);
		CharID b4 = new CharID(1, 4);
		doc.addChar(new CharNode(b1, null, 't'));
		doc.addChar(new CharNode(b2, b1, 'e'));
		doc.addChar(new CharNode(b3, b2, 's'));
		doc.addChar(new CharNode(b4, b3, 't'));
		System.out.println("Before replace: " + doc.getText());
		assert doc.getText().equals("test") : "Expected test";
		CharID b5 = new CharID(1, 5);
		CharNode nodeU = new CharNode(b5, b1, 'a');
		ReplaceCharOperation replaceOp = new ReplaceCharOperation("B1_1", b2, nodeU);
		replaceOp.apply(doc);
		System.out.println("After replacing 'e' with 'a': " + doc.getText());
		assert doc.getText().equals("tast") : "Expected tast";
		doc.printAll();
	}

	// ===== Member 3 tests (Block CRDT) =====

	// ------------------------------------------------------------------
	// Test 1 — basic block insert
	// Create 3 blocks in order, check document structure and full text
	// ------------------------------------------------------------------
	@Test
	void testBlockInsert() {
		System.out.println("===== Testing Member 3: Block Insert =====\n");

		BlockCRDT doc = new BlockCRDT();

		// Block 1: user 1 creates the first block (no parent)
		BlockID bid1 = new BlockID(1, 1);
		Block block1 = new Block(bid1, null);

		// add some characters into block 1 manually
		CharID c1 = new CharID(1, 1);
		CharID c2 = new CharID(1, 2);
		CharID c3 = new CharID(1, 3);
		block1.getContent().addChar(new CharNode(c1, null, 'H'));
		block1.getContent().addChar(new CharNode(c2, c1, 'i'));
		block1.getContent().addChar(new CharNode(c3, c2, '!'));

		// Block 2: user 1 creates a second block after block 1
		BlockID bid2 = new BlockID(1, 2);
		Block block2 = new Block(bid2, bid1);

		CharID c4 = new CharID(1, 4);
		CharID c5 = new CharID(1, 5);
		block2.getContent().addChar(new CharNode(c4, null, 'O'));
		block2.getContent().addChar(new CharNode(c5, c4, 'k'));

		// Block 3: user 1 creates a third block after block 2
		BlockID bid3 = new BlockID(1, 3);
		Block block3 = new Block(bid3, bid2);

		CharID c6 = new CharID(1, 6);
		block3.getContent().addChar(new CharNode(c6, null, '?'));

		// insert all three blocks using BlockOperation
		BlockOperation op1 = BlockOperation.insertBlock(block1);
		BlockOperation op2 = BlockOperation.insertBlock(block2);
		BlockOperation op3 = BlockOperation.insertBlock(block3);

		op1.apply(doc);
		op2.apply(doc);
		op3.apply(doc);

		System.out.println("After inserting 3 blocks:");
		doc.printAll();

		assert doc.getBlockCount() == 3 : "Expected 3 visible blocks, got " + doc.getBlockCount();
		assert doc.getFullText().equals("Hi!\nOk\n?") : "Expected 'Hi!\\nOk\\n?' got '" + doc.getFullText() + "'";

		System.out.println("Block insert test PASSED\n");
	}

	// ------------------------------------------------------------------
	// Test 2 — block delete (tombstone)
	// Insert 3 blocks, delete the middle one, check count and text
	// ------------------------------------------------------------------
	@Test
	void testBlockDelete() {
		System.out.println("===== Testing Member 3: Block Delete =====\n");

		BlockCRDT doc = new BlockCRDT();

		BlockID bid1 = new BlockID(1, 1);
		BlockID bid2 = new BlockID(1, 2);
		BlockID bid3 = new BlockID(1, 3);

		Block block1 = new Block(bid1, null);
		Block block2 = new Block(bid2, bid1);
		Block block3 = new Block(bid3, bid2);

		CharID c1 = new CharID(1, 1);
		block1.getContent().addChar(new CharNode(c1, null, 'A'));

		CharID c2 = new CharID(1, 2);
		block2.getContent().addChar(new CharNode(c2, null, 'B'));

		CharID c3 = new CharID(1, 3);
		block3.getContent().addChar(new CharNode(c3, null, 'C'));

		doc.addBlock(block1);
		doc.addBlock(block2);
		doc.addBlock(block3);

		System.out.println("Before delete:");
		doc.printAll();
		assert doc.getBlockCount() == 3 : "Expected 3 blocks before delete";

		// delete the middle block using BlockOperation
		BlockOperation deleteOp = BlockOperation.deleteBlock(bid2);
		deleteOp.apply(doc);

		System.out.println("After deleting block 2 (B):");
		doc.printAll();

		assert doc.getBlockCount() == 2 : "Expected 2 visible blocks after delete, got " + doc.getBlockCount();
		assert doc.getFullText().equals("A\nC") : "Expected 'A\\nC' got '" + doc.getFullText() + "'";

		// deleting the same block again should be idempotent (no crash)
		deleteOp.apply(doc);
		assert doc.getBlockCount() == 2 : "Re-deleting should not change count";

		System.out.println("Block delete test PASSED\n");
	}

	// ------------------------------------------------------------------
	// Test 3 — block split
	// Create a block with "Hello", split at index 2, expect "He" + "llo"
	// ------------------------------------------------------------------
	@Test
	void testBlockSplit() {
		System.out.println("===== Testing Member 3: Block Split =====\n");

		BlockCRDT doc = new BlockCRDT();

		BlockID bid1 = new BlockID(1, 1);
		Block block1 = new Block(bid1, null);

		// insert "Hello" into block 1
		CharID c1 = new CharID(1, 1);
		CharID c2 = new CharID(1, 2);
		CharID c3 = new CharID(1, 3);
		CharID c4 = new CharID(1, 4);
		CharID c5 = new CharID(1, 5);
		block1.getContent().addChar(new CharNode(c1, null, 'H'));
		block1.getContent().addChar(new CharNode(c2, c1, 'e'));
		block1.getContent().addChar(new CharNode(c3, c2, 'l'));
		block1.getContent().addChar(new CharNode(c4, c3, 'l'));
		block1.getContent().addChar(new CharNode(c5, c4, 'o'));

		doc.addBlock(block1);

		System.out.println("Before split: block text = '" + block1.getText() + "'");
		assert block1.getText().equals("Hello") : "Expected 'Hello' before split";

		// split at visible index 2 → "He" stays, "llo" moves to new block
		BlockID newBid = new BlockID(1, 2);   // user 1 creates their 2nd block
		BlockOperation splitOp = BlockOperation.splitBlock(bid1, 2, newBid);
		splitOp.apply(doc);

		System.out.println("After split at index 2:");
		doc.printAll();

		assert doc.getBlockCount() == 2 : "Expected 2 blocks after split, got " + doc.getBlockCount();

		Block first = doc.findBlock(bid1);
		Block second = doc.findBlock(newBid);

		assert first != null && !first.checkDeleted() : "First block should exist";
		assert second != null && !second.checkDeleted() : "Second block should exist";

		assert first.getText().equals("He") : "Expected first block = 'He', got '" + first.getText() + "'";
		assert second.getText().equals("llo") : "Expected second block = 'llo', got '" + second.getText() + "'";

		System.out.println("Block split test PASSED\n");
	}

	// ------------------------------------------------------------------
	// Test 4 — block merge
	// Create two blocks "Hello" and "World", merge them, expect "HelloWorld"
	// ------------------------------------------------------------------
	@Test
	void testBlockMerge() {
		System.out.println("===== Testing Member 3: Block Merge =====\n");

		BlockCRDT doc = new BlockCRDT();

		BlockID bid1 = new BlockID(1, 1);
		BlockID bid2 = new BlockID(1, 2);

		Block block1 = new Block(bid1, null);
		Block block2 = new Block(bid2, bid1);

		// "Hello" in block1
		CharID c1 = new CharID(1, 1);
		CharID c2 = new CharID(1, 2);
		CharID c3 = new CharID(1, 3);
		CharID c4 = new CharID(1, 4);
		CharID c5 = new CharID(1, 5);
		block1.getContent().addChar(new CharNode(c1, null, 'H'));
		block1.getContent().addChar(new CharNode(c2, c1, 'e'));
		block1.getContent().addChar(new CharNode(c3, c2, 'l'));
		block1.getContent().addChar(new CharNode(c4, c3, 'l'));
		block1.getContent().addChar(new CharNode(c5, c4, 'o'));

		// "World" in block2  (uses siteId 2 to avoid CharID collisions with block1)
		CharID c6 = new CharID(2, 1);
		CharID c7 = new CharID(2, 2);
		CharID c8 = new CharID(2, 3);
		CharID c9 = new CharID(2, 4);
		CharID c10 = new CharID(2, 5);
		block2.getContent().addChar(new CharNode(c6, null, 'W'));
		block2.getContent().addChar(new CharNode(c7, c6, 'o'));
		block2.getContent().addChar(new CharNode(c8, c7, 'r'));
		block2.getContent().addChar(new CharNode(c9, c8, 'l'));
		block2.getContent().addChar(new CharNode(c10, c9, 'd'));

		doc.addBlock(block1);
		doc.addBlock(block2);

		System.out.println("Before merge:");
		doc.printAll();

		assert doc.getBlockCount() == 2 : "Expected 2 blocks before merge";
		assert block1.getText().equals("Hello") : "Expected block1 = 'Hello'";
		assert block2.getText().equals("World") : "Expected block2 = 'World'";

		// merge block2 into block1
		BlockOperation mergeOp = BlockOperation.mergeBlocks(bid1, bid2);
		mergeOp.apply(doc);

		System.out.println("After merging block2 into block1:");
		doc.printAll();

		assert doc.getBlockCount() == 1 : "Expected 1 visible block after merge, got " + doc.getBlockCount();

		Block merged = doc.findBlock(bid1);
		assert merged != null && !merged.checkDeleted() : "Merged block should exist and not be deleted";
		assert merged.getText().equals("HelloWorld") : "Expected 'HelloWorld', got '" + merged.getText() + "'";

		System.out.println("Block merge test PASSED\n");
	}

	// ------------------------------------------------------------------
	// Test 5 — concurrent block insert (tie-breaker)
	// Two users insert a block at the same position simultaneously
	// Higher siteId should win and appear first (earlier in document)
	// ------------------------------------------------------------------
	@Test
	void testConcurrentBlockInsert() {
		System.out.println("===== Testing Member 3: Concurrent Block Insert (tie-breaker) =====\n");

		BlockCRDT doc = new BlockCRDT();

		// user 1 inserts the first block (anchor block — everyone agrees on this)
		BlockID bid1 = new BlockID(1, 1);
		Block anchor = new Block(bid1, null);
		CharID ca = new CharID(1, 1);
		anchor.getContent().addChar(new CharNode(ca, null, 'A'));
		doc.addBlock(anchor);

		// user 1 concurrently inserts a block after the anchor
		BlockID bidUser1 = new BlockID(1, 2);
		Block blockUser1 = new Block(bidUser1, bid1);
		CharID cx = new CharID(1, 2);
		blockUser1.getContent().addChar(new CharNode(cx, null, 'X'));

		// user 2 concurrently inserts a block after the same anchor
		BlockID bidUser2 = new BlockID(2, 1);
		Block blockUser2 = new Block(bidUser2, bid1);
		CharID cy = new CharID(2, 1);
		blockUser2.getContent().addChar(new CharNode(cy, null, 'Y'));

		// apply both — tie-breaker: higher siteId (user 2) goes first
		doc.addBlock(blockUser1);
		doc.addBlock(blockUser2);

		System.out.println("After concurrent block insert (user1='X', user2='Y' both after anchor 'A'):");
		System.out.println("Expected order: A → Y → X  (user 2 wins tie-breaker — higher siteId)");
		doc.printAll();

		assert doc.getBlockCount() == 3 : "Expected 3 visible blocks, got " + doc.getBlockCount();

		// check order by looking up each block's position directly
		// anchor=A should be first, user2=Y second (wins tie), user1=X third
		int posAnchor = -1, posUser1 = -1, posUser2 = -1;
		for (int i = 0; i < doc.allBlocks.size(); i++) {
			BlockID id = doc.allBlocks.get(i).getMyId();
			if (id.isSameAs(bid1))     posAnchor = i;
			if (id.isSameAs(bidUser1)) posUser1  = i;
			if (id.isSameAs(bidUser2)) posUser2  = i;
		}

		assert posAnchor < posUser2 : "Anchor block should come before user2 block";
		assert posUser2  < posUser1 : "User2 block should come before user1 block (higher siteId wins)";

		System.out.println("Concurrent block insert test PASSED\n");
	}

	// ------------------------------------------------------------------
	// Test 6 — split then merge round-trip
	// Split "Hello" into "He" + "llo", then merge back, expect "Hello"
	// ------------------------------------------------------------------
	@Test
	void testSplitThenMerge() {
		System.out.println("===== Testing Member 3: Split then Merge Round-Trip =====\n");

		BlockCRDT doc = new BlockCRDT();

		BlockID bid1 = new BlockID(1, 1);
		Block block1 = new Block(bid1, null);

		CharID c1 = new CharID(1, 1);
		CharID c2 = new CharID(1, 2);
		CharID c3 = new CharID(1, 3);
		CharID c4 = new CharID(1, 4);
		CharID c5 = new CharID(1, 5);
		block1.getContent().addChar(new CharNode(c1, null, 'H'));
		block1.getContent().addChar(new CharNode(c2, c1, 'e'));
		block1.getContent().addChar(new CharNode(c3, c2, 'l'));
		block1.getContent().addChar(new CharNode(c4, c3, 'l'));
		block1.getContent().addChar(new CharNode(c5, c4, 'o'));
		doc.addBlock(block1);

		// split at index 2
		BlockID bid2 = new BlockID(1, 2);
		doc.splitBlock(bid1, 2, bid2);

		System.out.println("After split:");
		doc.printAll();
		assert doc.getBlockCount() == 2 : "Expected 2 blocks after split";

		// now merge them back
		doc.mergeBlocks(bid1, bid2);

		System.out.println("After merging back:");
		doc.printAll();

		assert doc.getBlockCount() == 1 : "Expected 1 block after merge";
		assert doc.findBlock(bid1).getText().equals("Hello") : "Expected 'Hello' after merge, got '" + doc.findBlock(bid1).getText() + "'";

		System.out.println("Split-then-merge round-trip test PASSED\n");
	}
}