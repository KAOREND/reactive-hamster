package com.kaibla.hamster.persistence.query;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.kaibla.hamster.persistence.model.Document;
import java.io.Serializable;

/**
 *
 * @author kai
 */
public abstract class Condition implements Serializable {

    public abstract boolean isInCondition(Document o);
    
    public abstract void buildQuery(DBObject parentQuery);
    
    public void addToParentQuery(DBObject parentQuery,String field,Object value) {
        if(parentQuery.containsField(field)) {
            Object old = parentQuery.get(field);
            if(old instanceof BasicDBList) {
                BasicDBList l = (BasicDBList) old;
                l.add(value);
            } else {
                BasicDBList l= new BasicDBList();
                l.add(old);
                l.add(value);
                parentQuery.put(field, l);
            }
        } else if(parentQuery instanceof BasicDBList) {
            BasicDBList l=(BasicDBList) parentQuery;
            BasicDBObject n=  new BasicDBObject();
            n.put(field, value);
            l.add(n);
        }
         else {
           parentQuery.put(field, value); 
        }
    }
    

}
