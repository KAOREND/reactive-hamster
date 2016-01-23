/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * .
 */
package com.kaibla.hamster.components.list;

import com.kaibla.hamster.components.list.List;
import com.kaibla.hamster.base.DataModel;
import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.base.ModificationManager;
import com.kaibla.hamster.components.defaultcomponent.DefaultComponent;
import com.kaibla.hamster.persistence.query.Query;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.attribute.StringAttribute;
import com.kaibla.hamster.ui.test.UITest;
import static com.kaibla.hamster.ui.test.UITest.getLoader;
import java.io.Serializable;
import java.net.UnknownHostException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author kai
 */
public class ListTest extends UITest implements Serializable {

    public ListTest() {
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
    public void testListUpdate() {      
        HamsterPage page = createTestPage(testUIEngine);  
        ModificationManager m = page.getModificationManager();
        DefaultComponent d = new DefaultComponent(page); 
        //load components
        loadComponent(page, d);
        getLoader().loadComponent(List.class);
        
        page.addComponent(d);
        page.toString();
        final StringAttribute testName = new StringAttribute(testTable.getClass(), "name");
        List testList = new List(page,"testList", testTable.query(d,new Query()) ) {         
            
            @Override
            public HamsterComponent renderElement(DataModel data) {
                Document row=(Document) data;
                DefaultComponent el= new DefaultComponent(page); 
                el.addElement(row.get(testName));
                loadComponent(el);
                return el;
            }
        }; 
        d.addElement(testList);
       
        
        Document obj = testTable.createNew();
        obj.set(testName, "testEntry1");  
        obj.writeToDatabase();
        String t = m.getModificiationXML();
        System.out.println(""+t);
        assertTrue("testEntry1 should be visible ",t.contains("testEntry1"));      
        assertTrue("testEntry1 should be only once in the xml  ",t.split("testEntry1").length == 2);
        m.confirmLastModificationXML(m.getConfirmationCounter());
        
        t = m.getModificiationXML();
        System.out.println(""+t);
        assertFalse("testEntry1 should not be rendered twice ",t.contains("testEntry1"));
        
        m.confirmLastModificationXML(m.getConfirmationCounter());
        Document obj2 = testTable.createNew();
        obj2.set(testName, "testEntry2");  
        obj2.writeToDatabase();
        
        t = m.getModificiationXML();
        System.out.println(""+t);
        assertFalse("testEntry1 should not be rendered twice ",t.contains("testEntry1"));
        assertTrue("testEntry2 should be rendered  ",t.contains("testEntry2"));
        
    }
    
}
