package com.kaibla.hamster.example.ui;

import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.base.HamsterSession;
import com.kaibla.hamster.base.ModificationManager;
import com.kaibla.hamster.base.UIContext;
import com.kaibla.hamster.base.UIEngine;
import com.kaibla.hamster.example.ExampleChatEngine;
import com.kaibla.hamster.example.persistence.DocumentCollections;
import com.kaibla.hamster.ui.test.UITest;
import java.net.UnknownHostException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Example Unit test for the Chat. The test requires a running MongoDB instance.
 * 
 * @author kai
 */
public class ChatTest extends UITest {

    public ChatTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    @Override
    public void setUp() {
        super.setUp();
    }

    @Override
    public ExampleChatEngine createUITestEngine() {
        //create a modified version of our Engine
        ExampleChatEngine engine = new ExampleChatEngine() {
            //we want to use our test DB for JUnits
            @Override
            public void initDB() {
                //use the MongoDB "test"
                initDB("test");
            }
        };  
        return engine;
    }

    @Override
    public ExampleChatPage createTestPage(UIEngine engine) {
        //create a TestPage
        HamsterSession session = new HamsterSession(engine);
        session.setUser(testTable.createNew());
        ExampleChatPage page = new ExampleChatPage(engine, session);
        UIContext.setPage(page);
        Context.setListenerContainer(page.getListenerContainer());
        return page;
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testUpdateForNewMessage() throws InterruptedException {
        //This test will create new messages and will verify that
        // the messages would be rendered
        final ExampleChatPage page = createTestPage(testUIEngine);
        page.init();
        ModificationManager m = page.getModificationManager();
       
        //test 1: Add a message and verfiy that it is shown in the initial page
        DocumentCollections.MESSAGES.addMessage("TestMessage1");
        String intialHTML = page.getInitialHTMLCode();
        assertTrue(intialHTML + " did not contain TestMessage1", intialHTML.contains("TestMessage1"));
        System.out.println("initialHTML:\n" + intialHTML);
        
        //test 2: Add another message and test that is included in the resulting pageUpdate 
        DocumentCollections.MESSAGES.addMessage("TestMessage2");
        String ajaxDelta = m.getModificiationXML();
        assertTrue(ajaxDelta + " did not contain TestMessage2", ajaxDelta.contains("TestMessage2"));      
    }

}
