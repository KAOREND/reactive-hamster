package com.kaibla.hamster.persistence.query;

import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.model.Document;
import com.mongodb.client.model.Filters;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import org.bson.conversions.Bson;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class Greater extends UnaryCondition {

    public Greater(Attribute attr, Object value) {
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
        return attr.compare(value2, value) > 0;
    }

    @Override
    public Bson buildQuery() {
        return Filters.gt(attr.getName(), value);
    }

    @Override
    public Bson buildShadowQuery() {
         return Filters.or(Filters.gt(attr.getName(), value),Filters.gt(attr.getShadowName(), value));
    }
    
    

    @Override
    public boolean equals(Object o) {
        if (o instanceof Greater) {
            return super.equals(o);
        } else {
            return false;
        }
    }

    private static final Logger LOG = getLogger(Greater.class.getName());

}
