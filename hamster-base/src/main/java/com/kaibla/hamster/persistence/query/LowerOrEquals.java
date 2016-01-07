package com.kaibla.hamster.persistence.query;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.model.Document;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class LowerOrEquals extends UnaryCondition {

    public LowerOrEquals(Attribute attr, Object value) {
        super(attr, value);
    }

    @Override
    public boolean isInCondition(Document o) {
        Object value2 = o.get(attr);
        if (value2 == null && value == null) {
            return true;
        }
        if (value2 == null || value == null) {
            return false;
        }
        return attr.compare(value2, value) <= 0;
    }
    private static final Logger LOG = getLogger(LowerOrEquals.class.getName());

    @Override

    public void buildQuery(DBObject parentQuery) {
       BasicDBObject c = new BasicDBObject();
        c.put("$lte", value);
        addToParentQuery(parentQuery,attr.getName(), c);
    }   
    
    @Override
    public boolean equals(Object o) {
        if(o instanceof LowerOrEquals) {
            return super.equals(o);
        } else {
            return false;
        }
    }
}
