package com.kaibla.hamster.ui.test;

import com.mongodb.gridfs.GridFS;
import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterLoader;
import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.base.HamsterSession;
import com.kaibla.hamster.base.UIContext;
import com.kaibla.hamster.base.UIEngine;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.servlet.CometProcessor;
import com.kaibla.hamster.testutils.MongoDBTest;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import org.junit.After;
import org.junit.Before;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author korend
 */
public class UITest extends MongoDBTest {

    public static HamsterLoader loader;
    
    public UIEngine testUIEngine=null;
    

    
    @Before
    public void setUp() {
        super.setUp();
        testUIEngine=(UIEngine) testEngine;
        testEngine=testUIEngine;
        CometProcessor.setEngine(testUIEngine);
        UIContext.setUser(testCollection.createNewDummy());
    }
    
    @After
    public void tearDown() {
        super.tearDown();
        testUIEngine=null;
    }
    

    @Override
    public UIEngine createTestEngine() {
        if(testEngine !=null) {
            testEngine.destroy();
        }
        loader = new HamsterLoader();
        UIEngine engine = createUITestEngine();
        engine.initDB(db, GridFSBuckets.create(db));
        loader.setEngine(engine);
        testEngine=engine;
        CometProcessor.setEngine(engine);  
        return engine;
    }
    
     public  UIEngine createUITestEngine() {
         return new UIEngine() {

            @Override
            public HamsterPage createNewPage(HamsterSession session) {
                return createTestPage(this); 
            }

            @Override
            public boolean userExists(String userid, String userhash) {
                return true;
            }

            @Override
            public Locale getUserLocale(Document user) {
                return Locale.ENGLISH;
            }

            @Override
            public HamsterLoader getHamsterLoader() {
                return loader;
            }

            @Override
            public void destroy() {
                super.destroy(); //To change body of generated methods, choose Tools | Templates.
            }
        };
     }

    public HamsterPage createTestPage(UIEngine engine) {
        HamsterSession session = new HamsterSession(engine);
        session.setUser(testCollection.createNew());
        // session.setLastUserLogin(new Document());
        HamsterPage page = new TestPage(engine, session);
        UIContext.setPage(page);
        Context.setListenerContainer(page.getListenerContainer());
        return page;
    }

   

    public void loadComponent(HamsterComponent... comps) {

        for (HamsterComponent comp : comps) {
            loader.loadComponent(comp.getClass());
        }
    }

    public static HamsterLoader getLoader() {
        return loader;
    }

    public void loadComponentClass(Class... comps) {

        for (Class compClass : comps) {
            loader.loadComponent(compClass);
        }
    }
    private static final Logger LOG = getLogger(UITest.class.getName());
}
