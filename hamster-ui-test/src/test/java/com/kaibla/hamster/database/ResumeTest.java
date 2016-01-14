/*
 * 
 * .
 */
package com.kaibla.hamster.database;

import com.mongodb.gridfs.GridFS;
import com.kaibla.hamster.base.ChangedListener;
import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.components.defaultcomponent.DefaultComponent;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.attribute.StringAttribute;
import com.kaibla.hamster.ui.test.UITest;
import com.mongodb.client.gridfs.GridFSBuckets;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author kai
 */
public class ResumeTest extends UITest implements Serializable {
    
    public ResumeTest() {
    }

    /**
     * Test of writeReplace method, of class MongoObject.
     */
    @Test
    public void testWriteReplace() throws IOException, ClassNotFoundException {
        Document mo=testTable.createNew();
        ByteArrayOutputStream bout= new ByteArrayOutputStream();
        ObjectOutputStream out=new ObjectOutputStream(bout);
        out.writeObject(mo);
        out.close();
        ByteArrayInputStream bin= new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bin);
        Document deserialized=(Document) in.readObject();
        in.close();
        assertNotNull(deserialized);        
        assertTrue("MongoObjects should be the same after deserializing ",mo == deserialized);
    }
    
     @Test
    public void testWriteReplacePage() throws IOException, ClassNotFoundException {
        HamsterPage page = createTestPage(testUIEngine);        
        Document mo=testTable.createNew();
        TestComponent c = new TestComponent();
        c.m=mo;
        c.test="test";
        page.addComponent(c);
        testUIEngine.initDB(db,GridFSBuckets.create(db));
        testUIEngine.persistPage(page);
        HamsterPage p2= testUIEngine.resumePage( page.getId());
        assertNotNull(p2);
        TestComponent t=(TestComponent) p2.components.get(0);
        assertNotNull(t);
        assertEquals("test", t.test);
        assertNotNull(t.m);
        assertTrue(t.m == mo);
    }
     
     @Test
    public void testWriteReplaceComponent() throws IOException, ClassNotFoundException {
        Document mo=testTable.createNew();
        TestComponent c = new TestComponent();
        c.m=mo;
        c.test="test";
        c.setOneSlot(false);
        ByteArrayOutputStream bout= new ByteArrayOutputStream();
        ObjectOutputStream out=new ObjectOutputStream(bout);
        out.writeObject(c);
        out.close();
        ByteArrayInputStream bin= new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bin);
        TestComponent t=(TestComponent) in.readObject();
        in.close();
        assertNotNull(t);     
        assertNotNull(t.m);
        assertFalse(t.isOneSlot());
        assertTrue(t.m == mo);
    }
     
    public static class TestComponent extends DefaultComponent {
        Document m;
        String test;
        public TestComponent(HamsterPage page) {
            super(page);
        }

        public TestComponent() {
        }
    }
    private static final Logger LOG = getLogger(ResumeTest.class.getName());
}