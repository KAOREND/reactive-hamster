package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.transactions.*;
import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.persistence.attribute.IntegerAttribute;
import com.kaibla.hamster.persistence.query.BaseQuery;
import com.kaibla.hamster.persistence.query.Conditions;
import com.kaibla.hamster.persistence.query.Query;
import com.kaibla.hamster.testutils.MongoDBTest;
import java.util.Iterator;
import java.util.SortedSet;
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
        IntegerAttribute attr = new IntegerAttribute(testCollection.getClass(), "intattr1");
        Document doc1 = testCollection.createNew();
        doc1.set(attr, 1);
        doc1.writeToDatabase();

        Document doc2 = testCollection.createNew();
        doc2.set(attr, 2);
        doc2.writeToDatabase();

        Document doc3 = testCollection.createNew();
        doc3.set(attr, 3);
        doc3.writeToDatabase();

        Document doc4 = testCollection.createNew();
        doc4.set(attr, 4);
        doc4.writeToDatabase();

        DocumentCursor c = new DocumentCursor(new Query().addCondition(Conditions.gt(attr, 0)).addSortCriteria(attr, false), testCollection);
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
        IntegerAttribute attr = new IntegerAttribute(testCollection.getClass(), "intattr");
        Document doc1 = testCollection.createNew();
        doc1.set(attr, 1);
        doc1.writeToDatabase();

        Document doc2 = testCollection.createNew();
        doc2.set(attr, 2);
        doc2.writeToDatabase();

        Document doc3 = testCollection.createNew();
        doc3.set(attr, 3);
        doc3.writeToDatabase();

        Document doc4 = testCollection.createNew();
        doc4.set(attr, 4);
        doc4.writeToDatabase();

        ShadowAwareCursor c = new ShadowAwareCursor(new Query().addCondition(Conditions.gt(attr, 0)).addSortCriteria(attr, false), testCollection);
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

        c = new ShadowAwareCursor(new Query().addCondition(Conditions.gt(attr, 0)).addSortCriteria(attr, false), testCollection);
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
        DirtyCursor dirtyCursor = new DirtyCursor(new Query().addCondition(Conditions.gt(attr, 0)).addSortCriteria(attr, false), testCollection);
        iter = dirtyCursor.iterator();
        assertTrue(iter.hasNext());
        assertTrue(doc2 == iter.next());
        assertTrue(doc2.get(attr) == 2);
        assertTrue(iter.hasNext());
        assertTrue(doc3 == iter.next());
        assertTrue(doc3.get(attr) == 3);
        assertFalse(iter.hasNext());

        // t2 should still see the old version as t1 is not committed yet
        c = new ShadowAwareCursor(new Query().addCondition(Conditions.gt(attr, 0)).addSortCriteria(attr, false), testCollection);
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
        IntegerAttribute attr = new IntegerAttribute(testCollection.getClass(), "intattr4");
        Document doc1 = testCollection.createNew();
        doc1.set(attr, 1);
        doc1.writeToDatabase();

        Document doc2 = testCollection.createNew();
        doc2.set(attr, 2);
        doc2.writeToDatabase();

        Document doc3 = testCollection.createNew();
        doc3.set(attr, 3);
        doc3.writeToDatabase();

        Document doc4 = testCollection.createNew();
        doc4.set(attr, 4);
        doc4.writeToDatabase();

        ExtendableCursor c = new ExtendableCursor(testCollection, new Query().addSortCriteria(attr, false));
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

    @Test
    public void testNewDocumentTransaction() {

        TransactionManager tm = testEngine.getTransactionManager();
        Transaction t1 = tm.startTransaction();
        IntegerAttribute attr = new IntegerAttribute(testCollection.getClass(), "intattr5");
        Document doc1 = testCollection.createNew();
        doc1.set(attr, 1);
        doc1.writeToDatabase();

        Document doc2 = testCollection.createNew();
        doc2.set(attr, 2);
        doc2.writeToDatabase();

        Document doc3 = testCollection.createNew();
        doc3.set(attr, 3);
        doc3.writeToDatabase();

        Document doc4 = testCollection.createNew();
        doc4.set(attr, 4);
        doc4.writeToDatabase();

        Context.clear();
        Transaction t2 = tm.startTransaction();
        ShadowAwareCursor c = new ShadowAwareCursor(new Query().addSortCriteria(attr, false), testCollection);
        Iterator<Document> iter = c.iterator();
        //the newly created docs should not be visible yet       
        assertFalse(iter.hasNext());
        Context.clear();
        Context.setTransaction(t1);
        tm.commit();
        Context.clear();
        Transaction t3 = tm.startTransaction();
        Context.setTransaction(t3);
        //now the newly created docs should  be visible to a new transaction:     
        c = new ShadowAwareCursor(new Query().addSortCriteria(attr, false), testCollection);
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
    public void testQueryResultModel() {

        TransactionManager tm = testEngine.getTransactionManager();
        Transaction t1 = tm.startTransaction();
        IntegerAttribute attr = new IntegerAttribute(testCollection.getClass(), "intattr6");
        Document doc1 = testCollection.createNew();
        doc1.set(attr, 1);
        doc1.writeToDatabase();

        Document doc2 = testCollection.createNew();
        doc2.set(attr, 2);
        doc2.writeToDatabase();

        Document doc3 = testCollection.createNew();
        doc3.set(attr, 3);
        doc3.writeToDatabase();

        Document doc4 = testCollection.createNew();
        doc4.set(attr, 4);
        doc4.writeToDatabase();

        Document doc5 = testCollection.createNew();
        doc5.set(attr, 5);
        doc5.writeToDatabase();

        Document doc6 = testCollection.createNew();
        doc6.set(attr, 6);
        doc6.writeToDatabase();

        Document doc7 = testCollection.createNew();
        doc7.set(attr, 7);
        doc7.writeToDatabase();

        Document doc8 = testCollection.createNew();
        doc8.set(attr, 8);
        doc8.writeToDatabase();
        tm.commit();
        Context.clear();
        Transaction t2 = tm.startTransaction();
        
        QueryResultListModel qm= new QueryResultListModel(testCollection, t2, testCollection, new Query().addSortCriteria(attr, false));
        SortedSet<Document> s=qm.get(0, 2);
        assertTrue("expected size was 2 but it was: "+s.size(),s.size() == 2);
        assertTrue(s.contains(doc1));
        assertTrue(s.contains(doc2));
        s=qm.get(6, 2);
        assertTrue("expected size was 2 but it was: "+s.size(),s.size() == 2);
        assertTrue(s.contains(doc7));
        assertTrue(s.contains(doc8));
        
        
        qm= new QueryResultListModel(testCollection, t2, testCollection, new Query().addSortCriteria(attr, false));
        s=qm.get(0, 2);
        assertTrue("expected size was 2 but it was: "+s.size(),s.size() == 2);
        assertTrue(s.contains(doc1));
        assertTrue(s.contains(doc2));
        s=qm.get(2, 2);
        assertTrue("expected size was 2 but it was: "+s.size(),s.size() == 2);
        assertTrue(s.contains(doc3));
        assertTrue(s.contains(doc4));
    }

}
