
package com.kaibla.hamster.data;

import com.mongodb.DB;
import com.mongodb.gridfs.GridFS;
import com.kaibla.hamster.base.UIEngine;
import com.kaibla.hamster.persistence.query.Query;
import com.kaibla.hamster.persistence.model.DocumentCollection;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.attribute.DateAttribute;
import com.kaibla.hamster.persistence.attribute.StringAttribute;
import java.util.Date;
import java.util.List;


/**
 *
 * @author kai
 */
public class PersistedPages extends DocumentCollection {    
    public final static StringAttribute USERID = new StringAttribute(PersistedPages.class, "user");
    public final static StringAttribute FILENAME = new StringAttribute(PersistedPages.class, "filename");
    public final static DateAttribute CREATION_TIME = new DateAttribute(PersistedPages.class, "created");
    
    private GridFS gridFS;  
  
    private static final int maxPersistedPagesPerUser=10;
    private static final int minimumDelay =  30000;
    
    public PersistedPages(UIEngine engine, DB db, GridFS gridFS) {
        super(engine, db, "persistedPages");
        this.gridFS = gridFS;
        ensureIndex(false,true,USERID,CREATION_TIME);
    }
    
    public boolean checkAndCleanup(String userId, String fileName) {        
        List<Document> l=query(new Query().equals(USERID, userId).addOrder(CREATION_TIME, false));
        if(l.size() >= maxPersistedPagesPerUser) {
            Document oldest = l.iterator().next();            
            if((System.currentTimeMillis() - oldest.get(CREATION_TIME).getTime() ) < minimumDelay  ) {
                //there have been to many page persistences for this user in a short time, so don't persist
                return false;
            } else {
                //clean up oldest to free space for new persisted page
                gridFS.remove(oldest.get(FILENAME));
                oldest.delete();
            }
        }
        //create new entry
        Document newOne = createNew();
        newOne.set(USERID, userId);
        newOne.set(FILENAME,fileName);
        newOne.set(CREATION_TIME,new Date());
        newOne.writeToDatabase(false);
        return true;
    }    
}
