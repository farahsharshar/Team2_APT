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

	// ===== Member 2 tests (yours) =====

	@Test
	void testDelete() {
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
	void testReplace() {
		System.out.println("===== Testing Member 2: Replace =====\n");

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
}