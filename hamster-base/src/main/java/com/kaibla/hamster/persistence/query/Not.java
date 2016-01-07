
package com.kaibla.hamster.persistence.query;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.kaibla.hamster.persistence.model.Document;

/**
 *
 * @author kai
 */
public class Not extends Condition {
    Condition condition;

    @Override
    public boolean isInCondition(Document o) {
        return !condition.isInCondition(o);
    }

    @Override
    public void buildQuery(DBObject parentQuery) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
