package com.kaibla.hamster.persistence.query;

import com.kaibla.hamster.persistence.model.Document;
import com.mongodb.BasicDBList;
import java.io.Serializable;
import org.bson.conversions.Bson;

/**
 *
 * @author kai
 */
public abstract class Condition implements Serializable {

    public abstract boolean isInCondition(Document o);
    
    public abstract Bson buildQuery();
    
    

}
