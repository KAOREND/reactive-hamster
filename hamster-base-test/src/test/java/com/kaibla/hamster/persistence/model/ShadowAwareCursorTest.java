package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.transactions.*;
import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.persistence.attribute.IntegerAttribute;
import com.kaibla.hamster.persistence.query.BaseQuery;
import com.kaibla.hamster.persistence.query.Conditions;
import com.kaibla.hamster.persistence.query.Query;
import com.kaibla.hamster.testutils.MongoDBTest;
import java.util.Iterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kai
 */
public class ShadowAwareCursorTest extends MongoDBTest {

    public ShadowAwareCursorTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        super.setUp();
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testDocumentCursor() {
        IntegerAttribute attr = new IntegerAttribute(testTable.getClass(), "intattr1");
        Document doc1 = testTable.createNew();
        doc1.set(attr, 1);
        doc1.writeToDatabase();

        Document doc2 = testTable.createNew();
        doc2.set(attr, 2);
        doc2.writeToDatabase();

        Document doc3 = testTable.createNew();
        doc3.set(attr, 3);
        doc3.writeToDatabase();

        Document doc4 = testTable.createNew();
        doc4.set(attr, 4);
        doc4.writeToDatabase();

        DocumentCursor c = new DocumentCursor(new Query().addCondition(Conditions.gt(attr, 0)).addSortCriteria(attr, false), testTable);
        Iterator<Document> iter = c.iterator();

        assertTrue(iter.hasNext());
        assertTrue(doc1 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc2 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc3 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc4 == iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSimpleTransaction() {
        IntegerAttribute attr = new IntegerAttribute(testTable.getClass(), "intattr");
        Document doc1 = testTable.createNew();
        doc1.set(attr, 1);
        doc1.writeToDatabase();

        Document doc2 = testTable.createNew();
        doc2.set(attr, 2);
        doc2.writeToDatabase();

        Document doc3 = testTable.createNew();
        doc3.set(attr, 3);
        doc3.writeToDatabase();

        Document doc4 = testTable.createNew();
        doc4.set(attr, 4);
        doc4.writeToDatabase();

        ShadowAwareCursor c = new ShadowAwareCursor(new Query().addCondition(Conditions.gt(attr, 0)).addSortCriteria(attr, false), testTable);
        Iterator<Document> iter = c.iterator();

        assertTrue(iter.hasNext());
        assertTrue(doc1 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc2 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc3 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc4 == iter.next());
        assertFalse(iter.hasNext());

        TransactionManager tm = testEngine.getTransactionManager();
        Transaction t1 = tm.startTransaction();
        doc2.set(attr, 5);
        doc2.writeToDatabase();

        doc3.set(attr, 6);
        doc3.writeToDatabase();

        c = new ShadowAwareCursor(new Query().addCondition(Conditions.gt(attr, 0)).addSortCriteria(attr, false), testTable);
        iter = c.iterator();

        assertTrue(iter.hasNext());
        Document next = iter.next();
        assertTrue("next should have been doc1, instead it was " + next.getDataObject().toJson() + "  doc1 is: " + doc1.getDataObject().toJson(), doc1 == next);
        assertTrue(iter.hasNext());
        assertTrue(doc4 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc2 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc3 == iter.next());
        assertFalse(iter.hasNext());

        //switch to another transaction 
        Context.clear();

        Transaction t2 = tm.startTransaction();
        // t2 should still see the old order as t1 is not committed yet

        //first test if the Dirty Cursor works, it should now return doc2 and doc3 as they are dirty
        DirtyCursor dirtyCursor = new DirtyCursor(new Query().addCondition(Conditions.gt(attr, 0)).addSortCriteria(attr, false), testTable);
        iter = dirtyCursor.iterator();
        assertTrue(iter.hasNext());
        assertTrue(doc2 == iter.next());
        assertTrue(doc2.get(attr) == 2);
        assertTrue(iter.hasNext());
        assertTrue(doc3 == iter.next());
        assertTrue(doc3.get(attr) == 3);
        assertFalse(iter.hasNext());

        // t2 should still see the old version as t1 is not committed yet
        c = new ShadowAwareCursor(new Query().addCondition(Conditions.gt(attr, 0)).addSortCriteria(attr, false), testTable);
        iter = c.iterator();
        assertTrue(iter.hasNext());
        assertTrue(doc1 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc2 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc3 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc4 == iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testExtendableleCursor() {
        IntegerAttribute attr = new IntegerAttribute(testTable.getClass(), "intattr4");
        Document doc1 = testTable.createNew();
        doc1.set(attr, 1);
        doc1.writeToDatabase();

        Document doc2 = testTable.createNew();
        doc2.set(attr, 2);
        doc2.writeToDatabase();

        Document doc3 = testTable.createNew();
        doc3.set(attr, 3);
        doc3.writeToDatabase();

        Document doc4 = testTable.createNew();
        doc4.set(attr, 4);
        doc4.writeToDatabase();

        ExtendableCursor c = new ExtendableCursor(testTable, new Query().addSortCriteria(attr, false));
        c.setBlockSize(2);

        Iterator<Document> iter = c.iterator();

        assertTrue(iter.hasNext());
        assertTrue(doc1 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc2 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc3 == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(doc4 == iter.next());
        assertFalse(iter.hasNext());
    }

}
