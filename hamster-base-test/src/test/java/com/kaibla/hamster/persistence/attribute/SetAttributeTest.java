/*
 * 
 * .
 */
package com.kaibla.hamster.persistence.attribute;

import com.kaibla.hamster.testutils.MongoDBTest;
import com.kaibla.hamster.persistence.query.Query;
import com.kaibla.hamster.persistence.model.Document;
import static java.lang.System.currentTimeMillis;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author kai
 */
public class SetAttributeTest extends MongoDBTest {

    public SetAttributeTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    @Override
    public void setUp() throws UnknownHostException {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testSetAttribute() {
        Document testObject = testTable.createNew();
        SetAttribute setAttribute = new SetAttribute(testTable.getClass(), "test_set");
        HashSet testSet = new HashSet();
        testSet.add("hello");
        testObject.set(setAttribute, testSet);
        testObject.writeToDatabase();
        assert testObject.get(setAttribute).contains("hello");
    }

    @Test
    public void testSetQuery() {
        Document testObject = testTable.createNew();
        SetAttribute setAttribute = new SetAttribute(testTable.getClass(), "test_set2");
        HashSet testSet = new HashSet();
        testSet.add("apple");
        testSet.add("banana");
        testSet.add("black");
        testObject.set(setAttribute, testSet);
        testObject.writeToDatabase();

        Document queriedObject = testTable.queryOne(new Query().equals(setAttribute, "apple"));
        assert queriedObject != null;
        assert queriedObject.get(setAttribute).contains("apple");
        assert new Query().equals(setAttribute, "apple").isInQuery(queriedObject);
        assert !new Query().equals(setAttribute, "blue").isInQuery(queriedObject);
        Document nothing = testTable.queryOne(new Query().equals(setAttribute, "nothing"));
        assert nothing == null;

        LOG.info("Search with userset blue______________________________________");
        Iterator cursor = testTable.getCollection().find(new Query().equals(setAttribute, "apple").getQuery()).iterator();
        while (cursor.hasNext()) {
            org.bson.Document dbObject = (org.bson.Document) cursor.next();
            LOG.log(Level.INFO, "{0}", dbObject);
        }
    }

    public void testPerformance() {
        int searches = 2000;
        int users = 2;
        int tagsPerMessage = 60;
        int conversations = 2;
        int messages = 100;
        SetAttribute usersAttr = new SetAttribute(testTable.getClass(), "test_users");
        SetAttribute tagsAttr = new SetAttribute(testTable.getClass(), "test_tags");

        long startTime = currentTimeMillis();

        testTable.ensureIndex(true, false, usersAttr);
        testTable.ensureIndex(false, true, tagsAttr);
        for (int ci = 0; ci < conversations; ci++) {
            Document conversation = testTable.createNew();
            HashSet userSet = new HashSet();
            conversation.set(usersAttr, userSet);
            for (int ui = 1; ui < users + 1; ui++) {
                userSet.add("" + ui);
                int m = ui * tagsPerMessage;
                conversation.set(usersAttr, userSet);
                HashSet tagSet = new HashSet();
                for (int mi = 0; mi < messages; mi++) {
                    for (int ti = 0; ti < tagsPerMessage; ti++) {
                        tagSet.add("" + m++);
                    }
                    conversation.set(tagsAttr, tagSet);
                    conversation.writeToDatabase();
                }
            }
        }
        long processTime = currentTimeMillis() - startTime;
        int mTotal = conversations * users * messages;
        LOG.log(Level.INFO, "{0} write operations took {1}  {2} messages per ms  {3} ms per write", new Object[]{mTotal, processTime, mTotal / processTime, processTime / mTotal});
        Random r = new Random();
        startTime = currentTimeMillis();
        for (int i = 0; i < searches; i++) {
            int userId = r.nextInt(users) + 1;
            //equals(usersAttr, ""+userId)
            String tag = "" + (userId * tagsPerMessage + r.nextInt(tagsPerMessage * messages));
            Document test = testTable.queryOne(new Query().equals(usersAttr, "" + userId).equals(tagsAttr, tag));
//           assert test != null;
        }
        processTime = currentTimeMillis() - startTime;
        LOG.log(Level.INFO, "{0} search operations took {1}  {2} searches per ms {3}ms per search", new Object[]{searches, processTime, searches / processTime, processTime / searches});
    }

    private static final Logger LOG = getLogger(SetAttributeTest.class.getName());

}
