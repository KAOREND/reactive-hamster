package com.kaibla.hamster.persistence.query;

import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.attribute.DocumentReferenceAttribute;
import com.mongodb.client.model.Filters;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import org.bson.conversions.Bson;

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

    @Override
    public Bson buildQuery() {
       return buildQuery(attr.getName());
    }

    @Override
    public Bson buildShadowQuery() {
        return Filters.or(buildQuery(attr.getName()),buildQuery(attr.getShadowName()));
    }
    
    private Bson buildQuery(String attrName) {
         if (attr instanceof DocumentReferenceAttribute) {
            org.bson.Document s = new org.bson.Document();
            return Filters.ne(attrName, ((Document) value).getId());
        } else {
            return Filters.ne(attrName, value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof NotEquals) {
            return super.equals(o);
        } else {
            return false;
        }
    }

    private static final Logger LOG = getLogger(NotEquals.class.getName());
}
