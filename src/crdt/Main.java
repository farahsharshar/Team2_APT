package crdt;

public class Main {

    public static void main(String[] args) {

        System.out.println("===== Testing Member 1: Character Insert =====\n");

        // create the CRDT for one block
        CharCRDT myDoc = new CharCRDT();

        // each character needs: its own id, the id of the char before it, and the actual char

        // 'H' — first character, no parent (parentId = null)
        CharID id1 = new CharID(1, 1);
        CharNode nodeH = new CharNode(id1, null, 'H');

        // 'e' — comes after H
        CharID id2 = new CharID(1, 2);
        CharNode nodeE = new CharNode(id2, id1, 'e');

        // 'l' — comes after e
        CharID id3 = new CharID(1, 3);
        CharNode nodeL1 = new CharNode(id3, id2, 'l');

        // 'l' again
        CharID id4 = new CharID(1, 4);
        CharNode nodeL2 = new CharNode(id4, id3, 'l');

        // 'o'
        CharID id5 = new CharID(1, 5);
        CharNode nodeO = new CharNode(id5, id4, 'o');

        myDoc.addChar(nodeH);
        myDoc.addChar(nodeE);
        myDoc.addChar(nodeL1);
        myDoc.addChar(nodeL2);
        myDoc.addChar(nodeO);

        System.out.println("After user 1 types 'Hello':");
        myDoc.printAll();

        // test insert operation wrapper
        CharID id6 = new CharID(1, 6);
        CharNode nodeExclaim = new CharNode(id6, id5, '!');
        InsertOperation op = new InsertOperation("B1_1", nodeExclaim);
        System.out.println("\nCreated operation: " + op);
        myDoc.addChar(op.getCharToAdd());

        System.out.println("\nAfter adding '!':");
        System.out.println("Visible text: " + myDoc.getText());

        // ---- test : two users type at the same position ----
        System.out.println("\n===== Testing Concurrent Insert (tie-breaker) =====\n");

        CharCRDT doc2 = new CharCRDT();

        CharID userOneFirst = new CharID(1, 1);
        doc2.addChar(new CharNode(userOneFirst, null, 'A'));

        CharID userOneX = new CharID(1, 2);
        CharNode nodeX = new CharNode(userOneX, userOneFirst, 'X');

        CharID userTwoY = new CharID(2, 1);
        CharNode nodeY = new CharNode(userTwoY, userOneFirst, 'Y');

        doc2.addChar(nodeX);
        doc2.addChar(nodeY);

        System.out.println("After concurrent insert (user1='X', user2='Y' both after 'A'):");
        System.out.println("Expected: A Y X  (user 2 wins the tie because higher siteId)");
        System.out.println("Got:      " + doc2.getText());
        doc2.printAll();
    }
}
