package com.kaibla.hamster.persistence.query;

import com.kaibla.hamster.persistence.model.Document;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;

/**
 *
 * @author kai
 */
public class Not extends Condition {

    Condition condition;

    public Not(Condition condition) {
        this.condition = condition;
    }
    
    

    @Override
    public boolean isInCondition(Document o) {
        return !condition.isInCondition(o);
    }
    
    @Override
    public Bson buildQuery() {
        return Filters.not(condition.buildQuery());
    }

}
