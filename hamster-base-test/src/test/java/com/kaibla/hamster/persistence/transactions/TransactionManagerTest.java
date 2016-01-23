package com.kaibla.hamster.persistence.transactions;

import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.persistence.attribute.StringAttribute;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.OptimisticLockException;
import com.kaibla.hamster.testutils.MongoDBTest;
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
public class TransactionManagerTest extends MongoDBTest {

    public TransactionManagerTest() {
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
    public void testSimpleRollback() {
        StringAttribute text = new StringAttribute(testTable.getClass(), "text");
        Document doc = testTable.createNew();
        doc.set(text, "old value");
        doc.writeToDatabase();

        Context.clear();

        TransactionManager tm = testEngine.getTransactionManager();
        Transaction t1 = tm.startTransaction();
        doc.set(text, "new value");
        doc.writeToDatabase();
        tm.commit();
        assertTrue(doc.get(text).equals("new value"));
        Context.clear();
        assertTrue(doc.get(text).equals("new value"));

        //test rollback
        Transaction t2 = tm.startTransaction();
        doc.set(text, "rollback");
        doc.writeToDatabase();
        assertTrue(doc.get(text).equals("rollback"));
        tm.rollback();
        assertFalse(doc.get(text).equals("rollback"));
        Transaction t3 = tm.startTransaction();
        assertTrue(doc.get(text).equals("new value"));

    }

    @Test
    public void testSimpleIsolation() {
        StringAttribute text = new StringAttribute(testTable.getClass(), "text2");
        Document doc = testTable.createNew();
        doc.set(text, "old value");
        doc.writeToDatabase();
        Context.clear();

        TransactionManager tm = testEngine.getTransactionManager();
        Transaction t1 = tm.startTransaction();
        doc.set(text, "new value");
        doc.writeToDatabase();
        tm.commit();

        assertTrue(doc.get(text).equals("new value"));

        Context.clear();
        assertTrue(doc.get(text).equals("new value"));

        //test isolation
        Transaction t2 = tm.startTransaction();
        doc.set(text, "t2");
        doc.writeToDatabase();
        assertTrue(doc.get(text).equals("t2"));

        //switch to another transaction
        Context.clear();
        Transaction t3 = tm.startTransaction();
        assertTrue(doc.get(text).equals("new value"));

        Context.clear();
        Context.setTransaction(t2);
        tm.commit();
        // test what can be seen after the commit
        Context.clear();
        tm.startTransaction();
        assertTrue(doc.get(text).equals("t2"));
    }

    @Test
    public void testOptimisticLock() {
        StringAttribute text = new StringAttribute(testTable.getClass(), "text3");
        Document doc = testTable.createNew();
        doc.writeToDatabase();

        TransactionManager tm = testEngine.getTransactionManager();
        Transaction t1 = tm.startTransaction();
        doc.set(text, "t1");

        Context.clear();
        Transaction t2 = tm.startTransaction();
        doc.set(text, "t2");
        doc.writeToDatabase();

        Context.clear();
        Context.setTransaction(t1);
        boolean exception = false;
        try {
            doc.writeToDatabase();
        } catch (OptimisticLockException ex) {
            exception = true;
        }
        assertTrue(exception);
    }

    @Test
    public void testOptimisticLockUncommittedTransaction() {
        StringAttribute text = new StringAttribute(testTable.getClass(), "text4");
        Document doc = testTable.createNew();
        doc.writeToDatabase();

        TransactionManager tm = testEngine.getTransactionManager();
        Transaction t1 = tm.startTransaction();
        doc.set(text, "t1");
        doc.writeToDatabase();
        Context.clear();
        Transaction t2 = tm.startTransaction();
        doc.set(text, "t2");

        boolean exception = false;
        try {
            //this should throw an optimistic exception as t1 was not committed yet and we cannot overwrite dirty documents
            doc.writeToDatabase();
        } catch (OptimisticLockException ex) {
            exception = true;
        }
        assertTrue(exception);
    }

    @Test
    public void testOptimisticLockCommittedTransaction() {
        StringAttribute text = new StringAttribute(testTable.getClass(), "text5");
        Document doc = testTable.createNew();
        doc.writeToDatabase();
        TransactionManager tm = testEngine.getTransactionManager();
        Transaction t1 = tm.startTransaction();
        doc.set(text, "t1");
        doc.writeToDatabase();
        tm.commit();
        Context.clear();
        Transaction t2 = tm.startTransaction();
        doc.set(text, "t2");
        doc.writeToDatabase();
    }

    @Test
    public void testOptimisticLockRolledBackTransaction() {
        StringAttribute text = new StringAttribute(testTable.getClass(), "text6");
        Document doc = testTable.createNew();
        doc.writeToDatabase();

        TransactionManager tm = testEngine.getTransactionManager();
        Transaction t1 = tm.startTransaction();
        doc.set(text, "t1");
        doc.writeToDatabase();
        tm.rollback();
        Context.clear();
        Transaction t2 = tm.startTransaction();
        doc.set(text, "t2");
        doc.writeToDatabase();
    }

}
