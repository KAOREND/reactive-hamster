/*
 * 
 * .
 */
package com.kaibla.hamster.persistence;

import com.kaibla.hamster.persistence.query.Query;
import com.kaibla.hamster.persistence.query.BaseQuery;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.QueryResultListModel;
import com.kaibla.hamster.base.AbstractListenerContainer;
import com.kaibla.hamster.testutils.MongoDBTest;
import com.kaibla.hamster.base.ChangedListener;
import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.events.ListChangedEvent;
import com.kaibla.hamster.persistence.model.ListModel;
import com.kaibla.hamster.persistence.attribute.IntegerAttribute;
import static com.kaibla.hamster.testutils.Assertions.assertOrder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
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
public class TableListModelTest extends MongoDBTest {

    public TableListModelTest() {
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
    public void testOrder() {
        testTable.deleteContent();
        IntegerAttribute orderAttr = new IntegerAttribute(testTable.getClass(),"order");
        for (int i = 0; i < 10; i++) {
            Document mo = testTable.createNew();
            mo.set(orderAttr, i);
            mo.writeToDatabase();
        }
        BaseQuery orderQuery = new Query().addSortCriteria(orderAttr, false);
        int i = 0;
        for (Document mo : testTable.query(orderQuery)) {
            assertTrue(mo.get(orderAttr) == i);
            i++;
        }
        assertTrue(i == 10);
        i = 0;
        for (Iterator it = testTable.
                query(createTestListenerContainer(testEngine), orderQuery).get().
                iterator(); it.hasNext();) {
            Document mo = (Document) it.next();
            assertTrue(mo.get(orderAttr) == i);
            i++;
        }
        assertTrue(i == 10);
    }

    @Test
    public void testOrderChanged() {
        testTable.deleteContent();
        AbstractListenerContainer container=createTestListenerContainer(testEngine);
   
        
        IntegerAttribute orderAttr = new IntegerAttribute(testTable.getClass(),"order");
        Document mo1 = testTable.createNew();
        mo1.set(orderAttr, 1);
        mo1.writeToDatabase();
        Document mo2 = testTable.createNew();
        mo2.set(orderAttr, 2);
        mo2.writeToDatabase();
        Document mo3 = testTable.createNew();
        mo3.set(orderAttr, 3);
        mo3.writeToDatabase();
        Document mo4 = testTable.createNew();
        mo4.set(orderAttr, 4);
        mo4.writeToDatabase();
        BaseQuery orderQuery = new Query().addSortCriteria(orderAttr, false);
        QueryResultListModel testModel = testTable.query(container, orderQuery);
        List<Document> l1 = new ArrayList(testModel.get());
        final BooleanObject orderChanged=new BooleanObject(false);        
        
        testModel.addChangedListener(new ChangedListener() {

            @Override
            public void dataChanged(DataEvent e) {
                if(e instanceof ListChangedEvent) {
                    orderChanged.bool=true;
                }               
            }

            @Override
            public boolean isDestroyed() {
                return false;
            }
        });
        mo3.set(orderAttr, 7);
        mo3.writeToDatabase();
        assertTrue("order should have been changed",orderChanged.bool);
        assertOrder(testModel.get(),mo1,mo2,mo4,mo3);
        orderChanged.bool=false;
        mo3.set(orderAttr, 3);
        mo3.writeToDatabase();
        assertTrue("order should have been changed",orderChanged.bool);
        assertOrder(testTable.query(orderQuery),mo1,mo2,mo3,mo4);
        assertOrder(testModel.get(),mo1,mo2,mo3,mo4);
        orderChanged.bool=false;       
        mo3.writeToDatabase();
        assertFalse("order should not have been changed",orderChanged.bool);  
        assertOrder(testModel.get(),mo1,mo2,mo3,mo4);
    }
    
    
    @Test
    public void testNewEntry() {             
        AbstractListenerContainer container=createTestListenerContainer(testEngine);
        //we have to set the container into the context
        //otherwise the engine would execute the event handling 
        //asynchronously and not inside our own thread
        Context.setListenerContainer(container);
        ListModel lm = testTable.query(container,new Query());
        Document obj1 = testTable.createNew();      
        obj1.writeToDatabase();
        final BooleanObject changed=new BooleanObject(false);
        lm.addChangedListener(new ChangedListener() {

            @Override
            public void dataChanged(DataEvent e) {
              changed.bool=true;
            }

            @Override
            public boolean isDestroyed() {
                return false;
            }
        });
        Document obj2 = testTable.createNew();      
        obj2.writeToDatabase();
        assertTrue("listener should have been called after change",changed.bool);
    }
    
  
    
    private class BooleanObject {
        boolean bool;

        public BooleanObject(boolean bool) {
            this.bool = bool;
        }
        
    }
    private static final Logger LOG = getLogger(TableListModelTest.class.getName());
}