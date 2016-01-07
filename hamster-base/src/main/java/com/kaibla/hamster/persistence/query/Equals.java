package com.kaibla.hamster.persistence.query;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.attribute.DocumentReferenceAttribute;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author kai
 */
public class Equals extends UnaryCondition {

    public Equals(Attribute attr, Object value) {
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
        return attr.equals(value, value2);
    }
    private static final Logger LOG = getLogger(Equals.class.getName());

    @Override
    public void buildQuery(DBObject parentQuery) {
        if(attr instanceof DocumentReferenceAttribute) {
          addToParentQuery(parentQuery,attr.getName(), ((Document)value).getId());
        } else {
          addToParentQuery(parentQuery,attr.getName(), value);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if(o instanceof Equals) {
            return super.equals(o);
        } else {
            return false;
        }
    }

}
