/*
 * 
 * .
 */
package com.kaibla.hamster.persistence;

import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.testutils.MongoDBTest;
import com.kaibla.hamster.base.ChangedListener;
import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.persistence.attribute.StringAttribute;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author kai
 */
public class EntityTest extends MongoDBTest {
    
    public EntityTest() {
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
    public void testCleanUp() {
       Document test =testTable.createNew();
       StringAttribute testattr=new StringAttribute(testTable.getClass(), "test");
       ChangedListener testListener= new ChangedListener() {

           @Override
           public void dataChanged(DataEvent e) {
               
           }

           @Override
           public boolean isDestroyed() {
               return true;
           }
       };
       //test adding it as holder
       test.addHolder(testListener);
       test.cleanUp();
       assertFalse(test.hasListeners());
       
    }
    
   
    private static final Logger LOG = getLogger(EntityTest.class.getName());
}