/*
 * 
 * .
 */
package com.kaibla.hamster.testutils;

import com.kaibla.hamster.base.Context;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.model.DocumentCollection;
import com.kaibla.hamster.testutils.BaseTest;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
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
    public MongoDatabase db;
    

    @Before
    public void setUp() {
        Context.clear();
        MongoClient mongo = new MongoClient();
        db = mongo.getDatabase("mytest");
        db.drop();
        db = mongo.getDatabase("mytest");
        testEngine = createTestEngine();
        testEngine.init();        
        testTable = new DocumentCollection(testEngine, db, "testTable") {
            @Override
            public String getCollectionName() {
                return "testTable";
            }
        };
        testEngine.initDB(db);
    }

    @After
    public void tearDown() {
        db.drop();
        testEngine.destroy();
        testEngine=null;
    }
    
    
    private static final Logger LOG = getLogger(MongoDBTest.class.getName());

   
}
