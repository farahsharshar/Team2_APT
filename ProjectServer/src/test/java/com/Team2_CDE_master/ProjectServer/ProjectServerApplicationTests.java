package com.Team2_CDE_master.ProjectServer;

import com.Team2_CDE_master.ProjectServer.crdt.*;
import org.junit.jupiter.api.Test;

public class ProjectServerApplicationTests {

	// ===== Asmahan's tests =====

	@Test
	void testInsert() {
		System.out.println("===== Testing Character Insert =====\n");

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
	void testConcurrentInsert() {
		System.out.println("===== Testing Concurrent Insert =====\n");

		CharCRDT doc2 = new CharCRDT();

		CharID userOneFirst = new CharID(1, 1);
		doc2.addChar(new CharNode(userOneFirst, null, 'A'));

		CharID userOneX = new CharID(1, 2);
		CharNode nodeX = new CharNode(userOneX, userOneFirst, 'C');
		CharID userTwoY = new CharID(2, 1);
		CharNode nodeY = new CharNode(userTwoY, userOneFirst, 'B');

		doc2.addChar(nodeX);
		doc2.addChar(nodeY);

		System.out.println("Expected: ABC");
		System.out.println("Got: " + doc2.getText());
		assert doc2.getText().equals("ABC") : "Expected ABC";
		doc2.printAll();
	}

	// ===== Farah Sharshar's tests =====

	@Test
	void testDelete() {
		System.out.println("===== Test 1: Delete =====\n");

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
	void testReplace() {
		System.out.println("===== Test 2: Replace =====\n");

		CharCRDT doc = new CharCRDT();

		CharID b1 = new CharID(1, 1);
		CharID b2 = new CharID(1, 2);
		CharID b3 = new CharID(1, 3);

		doc.addChar(new CharNode(b1, null, 'c'));
		doc.addChar(new CharNode(b2, b1, 'a'));
		doc.addChar(new CharNode(b3, b2, 't'));

		System.out.println("Before replace: " + doc.getText());
		assert doc.getText().equals("cat") : "Expected cat";

		CharID b4 = new CharID(1, 4);
		CharNode nodeU = new CharNode(b4, b1, 'u');
		ReplaceCharOperation replaceOp = new ReplaceCharOperation("B1_1", b2, nodeU);
		replaceOp.apply(doc);

		System.out.println("After replacing 'a' with 'u': " + doc.getText());
		assert doc.getText().equals("cut") : "Expected cut";
		doc.printAll();
	}

	// ===== Rovana's tests (Block CRDT) =====

	// ------------------------------------------------------------------
	// Test 1: basic block insert
	// Create 3 blocks in order, check document structure and full text
	// ------------------------------------------------------------------
	@Test
	void testBlockInsert() {
		System.out.println("=====  Test 1: basic block insert =====\n");

		BlockCRDT doc = new BlockCRDT();

		// Block 1: user 1 creates the first block (no parent)
		BlockID bid1 = new BlockID(1, 1);
		Block block1 = new Block(bid1, null);

		// add some characters into block 1 manually
		CharID c1 = new CharID(1, 1);
		CharID c2 = new CharID(1, 2);
		CharID c3 = new CharID(1, 3);
		block1.getContent().addChar(new CharNode(c1, null, 'O'));
		block1.getContent().addChar(new CharNode(c2, c1, 'H'));
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
		assert doc.getFullText().equals("OH!\nOk\n?") : "Expected 'OH!\\nOk\\n?' got '" + doc.getFullText() + "'";

		System.out.println("Block insert test PASSED\n");
	}

	// ------------------------------------------------------------------
	// Test 2: block delete (tombstone)
	// Insert 3 blocks, delete the middle one, check count and text
	// ------------------------------------------------------------------
	@Test
	void testBlockDelete() {
		System.out.println("===== Test 2: block delete (tombstone) =====\n");

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
	// Test 3: block split
	// Create a block with "Hello", split at index 2, expect "He" + "llo"
	// ------------------------------------------------------------------
	@Test
	void testBlockSplit() {
		System.out.println("===== Test 3: block split =====\n");

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
	// Test 4: block merge
	// Create two blocks "Hello" and "World", merge them, expect "HelloWorld"
	// ------------------------------------------------------------------
	@Test
	void testBlockMerge() {
		System.out.println("===== Test 4: block merge =====\n");

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
	// Test 5: concurrent block insert (tie-breaker)
	// Two users insert a block at the same position simultaneously
	// Higher siteId should win and appear first (earlier in document)
	// ------------------------------------------------------------------
	@Test
	void testConcurrentBlockInsert() {
		System.out.println("===== Test 5: concurrent block insert (tie-breaker) =====\n");

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
	// Test 6: split then merge round-trip
	// Split "Hello" into "He" + "llo", then merge back, expect "Hello"
	// ------------------------------------------------------------------
	@Test
	void testSplitThenMerge() {
		System.out.println("===== Test 6: split then merge round-trip =====\n");

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

//tests formatting (Farah Elhebeishy):

// Test A: same parent, different siteId
// Two users type at the same spot at the same time
// Higher siteId should always end up on the left (aka typed first)
	@Test
	void testDeterministicOrderBySiteId() {
		System.out.println("===== Test A: Deterministic Order by siteId =====\n");

		CharCRDT doc = new CharCRDT();

		// first user types "A"
		CharID anchorId = new CharID(1, 1);
		doc.addChar(new CharNode(anchorId, null, 'A'));

		// both users type at the same exact time (after the first 'A')
		// 2nd user's entered should go first (cuz higher siteID)
		CharID user1Id = new CharID(1, 2);
		CharID user2Id = new CharID(2, 1);

		CharNode nodeFromUser1 = new CharNode(user1Id, anchorId, 'X');
		CharNode nodeFromUser2 = new CharNode(user2Id, anchorId, 'Y');

		// assume we got user1's first (to show the ordering)
		doc.addChar(nodeFromUser1);
		doc.addChar(nodeFromUser2);

		System.out.println("Expected: AYX");
		System.out.println("Got: " + doc.getText());

		assert doc.getText().equals("AYX") : "Expected AYX, got: " + doc.getText();

		System.out.println("Farah's Test A PASSED :)\n");
	}

// Test B: same parent, same siteId, different myNum
// this only happens in edge cases but we still need deterministic order
// higher myNum should be typed first
	@Test
	void testDeterministicOrderByMyNum() {
		System.out.println("===== Test B: Deterministic Order by myNum =====\n");

		CharCRDT doc = new CharCRDT();


		CharID anchorId = new CharID(1, 1);
		doc.addChar(new CharNode(anchorId, null, 'A'));

		// same siteId=1 and dif myNUm (highNum should be tyoed first)
		CharID lowNum  = new CharID(1, 2);   // myNum=2
		CharID highNum = new CharID(1, 5);   // myNum=5

		CharNode nodeLow  = new CharNode(lowNum,  anchorId, 'L');
		CharNode nodeHigh = new CharNode(highNum, anchorId, 'H');

		doc.addChar(nodeLow);
		doc.addChar(nodeHigh);//high should still end up first

		System.out.println("Expected: AHL");
		System.out.println("Got: " + doc.getText());

		assert doc.getText().equals("AHL") : "Expected AHL, got: " + doc.getText();

		System.out.println("Farah's Test B PASSED :)\n");
	}

// Test C: insert order should not matter
// Insert the same two concurrent nodes in reversed order
// Should get the same result both times
	@Test
	void testDeterministicOrderIsStable() {
		System.out.println("===== Test C: Order is Stable Regardless of Insert Order =====\n");

		// first doc: insert user2 first, then user1
		CharCRDT docA = new CharCRDT();
		CharID anchor_A = new CharID(1, 1);
		docA.addChar(new CharNode(anchor_A, null, 'A'));

		CharID u1_A = new CharID(1, 2);
		CharID u2_A = new CharID(2, 1);
		docA.addChar(new CharNode(u2_A, anchor_A, 'Y'));   // user2 inserted first
		docA.addChar(new CharNode(u1_A, anchor_A, 'X'));   // user1 inserted second

		// 2nd doc: insert user1 first, then user2 (reversed)
		CharCRDT docB = new CharCRDT();
		CharID anchor_B = new CharID(1, 1);
		docB.addChar(new CharNode(anchor_B, null, 'A'));

		CharID u1_B = new CharID(1, 2);
		CharID u2_B = new CharID(2, 1);//should still print first no matter what
		docB.addChar(new CharNode(u1_B, anchor_B, 'X'));   // user1 inserted first
		docB.addChar(new CharNode(u2_B, anchor_B, 'Y'));   // user2 inserted second
//the expected is that it will always be the same order no matter nsertion because the order
		//is based on siteID not the "insert order"
		//this is basically making sure it is robust
		System.out.println("Doc A (inserted user2 first): " + docA.getText());
		System.out.println("Doc B (inserted user1 first): " + docB.getText());

		// both docs must end up the same
		assert docA.getText().equals(docB.getText()) : "Expected same text in both docs, got " + docA.getText() + " vs " + docB.getText();
		assert docA.getText().equals("AYX") : "Expected AYX, got " + docA.getText();

		System.out.println("Farah's Test C PASSED :)\n");
	}

// Test D: apply bold to one character
// just making sure setBold works and only the highlightd character is bold
	@Test
	void testApplyBold() {
		System.out.println("===== Test D: Apply Bold =====\n");

		CharCRDT doc = new CharCRDT();

		CharID id1 = new CharID(1, 1);
		CharID id2 = new CharID(1, 2);
		CharID id3 = new CharID(1, 3);

		CharNode nodeH = new CharNode(id1, null,  'H');
		CharNode nodeI = new CharNode(id2, id1,   'i');
		CharNode nodeExc = new CharNode(id3, id2,   '!');

		doc.addChar(nodeH);
		doc.addChar(nodeI);
		doc.addChar(nodeExc);

		// bold 'i'
		FormattingOperation boldOp = new FormattingOperation( id2, "bold", true);
		boldOp.apply(doc);

		System.out.println("After bolding 'i':");
		doc.printAll();

		// only 'i' should be bold
		assert doc.findNode(id1).checkBold() == false : "H should not be bold";
		assert doc.findNode(id2).checkBold() == true  : "i should be bold";
		assert doc.findNode(id3).checkBold() == false : "! should not be bold";

		System.out.println("Farah's Test D PASSED :)\n");
	}

// Test E: applying italic to one character
// same idea as bold test bs for italic
	@Test
	void testApplyItalic() {
		System.out.println("===== Test E: Apply Italic =====\n");

		CharCRDT doc = new CharCRDT();

		CharID id1 = new CharID(1, 1);
		CharID id2 = new CharID(1, 2);

		doc.addChar(new CharNode(id1, null, 'O'));
		doc.addChar(new CharNode(id2, id1,  'k'));

		// make 'O' italic
		FormattingOperation italicOp = new FormattingOperation(id1, "italic", true);
		italicOp.apply(doc);

		System.out.println("After italicizing 'O':");
		doc.printAll();

		assert doc.findNode(id1).checkItalic() == true  : "O should be italic";
		assert doc.findNode(id2).checkItalic() == false : "k should not be italic";

		System.out.println("Farah's Test E PASSED :)\n");
	}

// Test F: apply both bold AND italic to the same character
// make sure one does not overwrite the other
	@Test
	void testApplyBoldAndItalicSameChar() {
		System.out.println("===== Test F: Bold and Italic on Same Char =====\n");

		CharCRDT doc = new CharCRDT();

		CharID id1 = new CharID(1, 1);
		CharID id2 = new CharID(1, 2);
		CharID id3 = new CharID(1, 3);

		doc.addChar(new CharNode(id1, null, 'H'));
		doc.addChar(new CharNode(id2, id1,  'i'));
		doc.addChar(new CharNode(id3, id2,  '!'));

		// bold 'i' first then italic it
		// (both should be true at the end)
		FormattingOperation boldOp   = new FormattingOperation( id2, "bold",   true);
		FormattingOperation italicOp = new FormattingOperation(id2, "italic", true);
		boldOp.apply(doc);
		italicOp.apply(doc);

		System.out.println("After bold + italic on 'i':");
		doc.printAll();

		assert doc.findNode(id2).checkBold()   == true  : "i should be bold";
		assert doc.findNode(id2).checkItalic() == true  : "i should also be italic";

		// make sure the other chars were not touched
		assert doc.findNode(id1).checkBold()   == false : "H should not be bold";
		assert doc.findNode(id1).checkItalic() == false : "H should not be italic";
		assert doc.findNode(id3).checkBold()   == false : "! should not be bold";
		assert doc.findNode(id3).checkItalic() == false : "! should not be italic";

		System.out.println("Farah's Test F PASSED :)\n");
	}

// Test G: turn bold off after turning it on
// basically making sure we can toggle formatting back to false
	@Test
	void testRemoveBold() {
		System.out.println("===== Test G: Remove Bold =====\n");

		CharCRDT doc = new CharCRDT();

		CharID id1 = new CharID(1, 1);
		doc.addChar(new CharNode(id1, null, 'Z'));

		// turn bold on
		FormattingOperation boldOn = new FormattingOperation( id1, "bold", true);
		boldOn.apply(doc);
		assert doc.findNode(id1).checkBold() == true : "Z should be bold after turning on";

		// turn bold off
		FormattingOperation boldOff = new FormattingOperation(id1, "bold", false);
		boldOff.apply(doc);
		assert doc.findNode(id1).checkBold() == false : "Z should not be bold after turning off";

		System.out.println("we turned bold on then off:");
		doc.printAll();

		System.out.println("Farah's Test G PASSED :)\n");
	}

// Test H: formatting on a deleted character should do nothing
	@Test
	void testFormattingOnDeletedNodeDoesNothing() {
		System.out.println("===== Test H: Formatting on Deleted Node Does Nothing =====\n");

		CharCRDT doc = new CharCRDT();

		CharID id1 = new CharID(1, 1);
		CharNode node = new CharNode(id1, null, 'X');
		doc.addChar(node);


		node.markDeleted();
		assert node.checkDeleted() == true : "Node should be deleted";

		// now try to bold it (should be ignored)
		FormattingOperation boldOp = new FormattingOperation(id1, "bold", true);
		boldOp.apply(doc);

		// bold should still be false because the node was deleted
		assert doc.findNode(id1).checkBold() == false : "Deleted node should not get bold applied";

		System.out.println("After trying to bold a deleted node:");
		doc.printAll();

		System.out.println("Farah's Test H PASSED :)\n");
	}

// Test I: formatting on a node that does not exist should not crash
//not sure when we'll actually use this tho but might as well
//note to self: ask the TA whether this is the correct logic or not cuz it makes sense
//in my head but I'm not if this is correct or not
	@Test
	void testFormattingOnMissingNodeDoesNotCrash() {
		System.out.println("===== Test I: Formatting on Missing Node Does Not Crash =====\n");

		CharCRDT doc = new CharCRDT();

		CharID id1 = new CharID(1, 1);
		doc.addChar(new CharNode(id1, null, 'A'));

		// try to format a node that never existed
		CharID fakeId = new CharID(9, 99);
		FormattingOperation boldOp = new FormattingOperation(fakeId, "bold", true);
		boldOp.apply(doc);   // should not throw any exception

		System.out.println("didnt crash - test passed.");
		//note to self: ask the TA whether this is the correct logic or not cuz it makes sense
		//in my head but I'm not if this is correct or not
		System.out.println("Farah's Test I PASSED :)\n");
	}


//#note to self: in the future when dealing with bold and italics
// the way to go about it is to loop over the selected bunch of characters to be bold/italics
//and loop over them applying italic/bold
//something like this:

// note test
// simulates the user selecting "ell" inside "Hello" and hitting bold
@Test
void testApplyBoldOnSelectedText() {
	System.out.println("===== Bold on Selected Text =====\n");

	CharCRDT doc = new CharCRDT();

	CharID id1 = new CharID(1, 1);
	CharID id2 = new CharID(1, 2);
	CharID id3 = new CharID(1, 3);
	CharID id4 = new CharID(1, 4);
	CharID id5 = new CharID(1, 5);

	doc.addChar(new CharNode(id1, null, 'H'));
	doc.addChar(new CharNode(id2, id1,  'e'));
	doc.addChar(new CharNode(id3, id2,  'l'));
	doc.addChar(new CharNode(id4, id3,  'l'));
	doc.addChar(new CharNode(id5, id4,  'o'));

	// selection = "ell" = id2, id3, id4
	CharID[] selection = { id2, id3, id4 }; //assume this is the seleted ids
	// i'd loop over the selected ids and apply one by one
	for (CharID selectedId : selection) {
		FormattingOperation boldOp = new FormattingOperation(selectedId, "bold", true);
		boldOp.apply(doc);
	}

	System.out.println("After bolding 'ell' in 'Hello':");
	doc.printAll();

	// H and o should NOT be bold
	assert doc.findNode(id1).checkBold() == false : "H should not be bold";
	assert doc.findNode(id5).checkBold() == false : "o should not be bold";

	// e, l, l should all be bold
	assert doc.findNode(id2).checkBold() == true : "e should be bold";
	assert doc.findNode(id3).checkBold() == true : "first l should be bold";
	assert doc.findNode(id4).checkBold() == true : "second l should be bold";

	System.out.println("Farah's Test note PASSED\n");
}

}
