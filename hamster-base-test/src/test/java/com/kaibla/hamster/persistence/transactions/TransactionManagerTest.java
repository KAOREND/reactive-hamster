package com.kaibla.hamster.persistence.transactions;

import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.persistence.attribute.StringAttribute;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.OptimisticLockException;
import com.kaibla.hamster.persistence.query.Conditions;
import com.kaibla.hamster.persistence.query.Query;
import com.kaibla.hamster.testutils.MongoDBTest;
import java.util.Date;
import org.bson.types.ObjectId;
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
        StringAttribute text = new StringAttribute(testCollection.getClass(), "text");
        Document doc = testCollection.createNew();
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
        StringAttribute text = new StringAttribute(testCollection.getClass(), "text2");
        Document doc = testCollection.createNew();
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
        StringAttribute text = new StringAttribute(testCollection.getClass(), "text3");
        Document doc = testCollection.createNew();
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
        StringAttribute text = new StringAttribute(testCollection.getClass(), "text4");
        Document doc = testCollection.createNew();
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
        StringAttribute text = new StringAttribute(testCollection.getClass(), "text5");
        Document doc = testCollection.createNew();
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
        StringAttribute text = new StringAttribute(testCollection.getClass(), "text6");
        Document doc = testCollection.createNew();
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

    @Test
    public void testRunInTransaction() {
        final StringAttribute text = new StringAttribute(testCollection.getClass(), "text7");
        final Document doc = testCollection.createNew();
        doc.writeToDatabase();

        TransactionManager tm = testEngine.getTransactionManager();
        Transaction t1 = tm.startTransaction();
        doc.set(text, "t1");
        doc.writeToDatabase();

        Context.clear();
        boolean rolledback = false;
        try {
            tm.runInTransaction(new Runnable() {
                @Override
                public void run() {
                    doc.set(text, "t2");
                    doc.writeToDatabase();
                }
            });
        } catch (RollbackException ex) {
            rolledback = true;
        }
        assertTrue(rolledback);
        tm.runInTransaction(new Runnable() {
            @Override
            public void run() {
                assertTrue(doc.get(text) == null);
            }
        });
        Context.setTransaction(t1);
        tm.commit();
        tm.runInTransaction(new Runnable() {
            @Override
            public void run() {
                assertTrue(doc.get(text).equalsIgnoreCase("t1"));
            }
        });
    }

    @Test
    public void testRollbackCreation() {
        final StringAttribute text = new StringAttribute(testCollection.getClass(), "text8");
        assertTrue(testCollection.getSize() == 0);

        TransactionManager tm = testEngine.getTransactionManager();
        try {
            tm.runInTransaction(new Runnable() {
                @Override
                public void run() {
                    final Document doc = testCollection.createNew();
                    doc.writeToDatabase();
                    assertTrue(testCollection.getSize() == 1);
                    throw new OptimisticLockException(doc);
                }
            });
        } catch (RollbackException ex) {
            ex.printStackTrace();
        }
        assertTrue(testCollection.getSize() == 0);
    }

    static final ThreadLocal isItMe = new ThreadLocal();

    private Document createDebugDocument() {

        org.bson.Document newData = new org.bson.Document();
        ObjectId id = new ObjectId(new Date());
        newData.put("_id", id);
        Document newObject = new Document(testEngine, testCollection, newData) {
            @Override
            public synchronized void writeToDatabase() {
                if (isItMe.get() != null) {
                    super.writeToDatabase();
                } else {
                    throw new RuntimeException("this should not have been called by another thread. Who was it?");
                }
            }

        };
        newObject.setNew(true);
        testCollection.addToCache(newObject);
        return newObject;
    }

    @Test
    public void testRollbackCreation2() {
        final StringAttribute text = new StringAttribute(testCollection.getClass(), "text9");
        assertTrue(testCollection.getSize() == 0);
        isItMe.set(this);
//        for (int i = 0; i < 20; i++) {
            testCollection.deleteContent();
            Context.clear();
            final TransactionManager tm = testEngine.getTransactionManager();
            tm.startTransaction();
            final Document doc1 = createDebugDocument();
            doc1.writeToDatabase();
            tm.commit();
            Context.clear();
            final Transaction t1 = tm.startTransaction();
            doc1.set(text, "uncommitted");
            doc1.writeToDatabase();
            Context.clear();
//            try {
            tm.runInTransaction(new Runnable() {
                @Override
                public void run() {
                    if (Context.getTransaction().getRetriesLeft() == 1) {
                        tm.commit(t1);
                    }
                    final Document doc = testCollection.createNew();
                    doc.set(text, "newone" + Context.getTransaction().getRetriesLeft());
                    doc.writeToDatabase();
                    doc1.set(text, "" + Context.getTransaction().getRetriesLeft());
                    doc1.writeToDatabase();
                }
            }, 5);
            

            assertTrue("testTable size was: " + testCollection.getSize(), testCollection.getSize() == 2);
            assertTrue(doc1.get(text).equals("1"));
//        }
    }

    @Test
    public void testDeletion() {
        StringAttribute text = new StringAttribute(testCollection.getClass(), "something");
        Document doc = testCollection.createNew();
        doc.writeToDatabase();

        TransactionManager tm = testEngine.getTransactionManager();
        Transaction t1 = tm.startTransaction();
        doc.set(text, "t1");
        doc.writeToDatabase();
        tm.commit();

        assertTrue(testCollection.queryOne(new Query().addCondition(
                Conditions.eq(text, "t1"))) != null);

        Context.clear();
        Transaction t2 = tm.startTransaction();
        doc.delete();
        assertTrue(testCollection.queryOne(new Query().addCondition(
                Conditions.eq(text, "t1")
        ))
                == null);

        Context.clear();
        Transaction t3 = tm.startTransaction();
        assertTrue(testCollection.queryOne(new Query().addCondition(
                Conditions.eq(text, "t1")
        ))
                != null);
        Context.setTransaction(t2);
        tm.commit();
        Transaction t4 = tm.startTransaction();
        assertTrue(testCollection.queryOne(new Query().addCondition(
                Conditions.eq(text, "t1")
        ))
                == null);
    }

}
