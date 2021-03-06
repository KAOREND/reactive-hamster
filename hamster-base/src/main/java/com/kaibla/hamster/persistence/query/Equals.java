package com.kaibla.hamster.persistence.query;

import com.mongodb.DBObject;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.attribute.DocumentReferenceAttribute;
import com.mongodb.client.model.Filters;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import org.bson.conversions.Bson;
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
    
    @Override
    public Bson buildQuery() {
        return buildQuery(attr.getName());
    }
    
    
     public Bson buildQuery(String attrName) {
         if(attr instanceof DocumentReferenceAttribute) {
          return Filters.eq(attrName, ((Document)value).getId());
        } else {
            return Filters.eq(attrName, value);
        }
    }
    
    
    @Override
    public Bson buildShadowQuery() {
        return Filters.or(buildQuery(attr.getName()),buildQuery(attr.getShadowName()));
    }
    
    
    
    @Override
    public boolean equals(Object o) {
        if(o instanceof Equals) {
            return super.equals(o);
        } else {
            return false;
        }
    }
    
    private static final Logger LOG = getLogger(Equals.class.getName());

   
}
