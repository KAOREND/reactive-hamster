/*
 * 
 * .
 */
package com.kaibla.hamster.testutils;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.model.DocumentCollection;
import com.kaibla.hamster.testutils.BaseTest;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author kai
 */
public class MongoDBTest extends BaseTest {

    public DocumentCollection testTable;
    public DB db;
    

    @Before
    public void setUp() throws UnknownHostException {

        Mongo mongo = new Mongo();
        db = mongo.getDB("test");
        db.dropDatabase();
        db = mongo.getDB("test");
        testEngine = createTestEngine();
        testEngine.init();        
        testTable = new DocumentCollection(testEngine, db, "testTable") {
            @Override
            public String getTableName() {
                return "testTable";
            }
        };
    }

    @After
    public void tearDown() {
        db.dropDatabase();
        testEngine.destroy();
        testEngine=null;
    }
    private static final Logger LOG = getLogger(MongoDBTest.class.getName());

   
}
