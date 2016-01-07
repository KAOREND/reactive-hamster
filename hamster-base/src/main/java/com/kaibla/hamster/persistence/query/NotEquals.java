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
public class NotEquals extends UnaryCondition {

    public NotEquals(Attribute attr, Object value) {
        super(attr, value);
    }

    @Override
    public boolean isInCondition(Document o) {
        Object value2 = o.get(attr);
        if (value2 == null && value == null) {
            return false;
        }
        if (value2 == null || value == null) {
            return true;
        }
        return !attr.equals(value, value2);
    }
    private static final Logger LOG = getLogger(NotEquals.class.getName());

    @Override
    public void buildQuery(DBObject parentQuery) {

        if (attr instanceof DocumentReferenceAttribute) {           
            BasicDBObject s = new BasicDBObject();
            s.put("$ne", ((Document) value).getId());
            addToParentQuery(parentQuery,attr.getName(), s);
        } else {
            BasicDBObject s = new BasicDBObject();
            s.put("$ne", value);
            addToParentQuery(parentQuery,attr.getName(), s);
        }
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof NotEquals) {
            return super.equals(o);
        } else {
            return false;
        }
    }
}
