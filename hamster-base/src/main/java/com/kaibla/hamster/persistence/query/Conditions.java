package com.kaibla.hamster.persistence.query;

import com.kaibla.hamster.persistence.attribute.Attribute;
import java.util.Arrays;

/**
 * Builder class for query conditions.
 * 
 * @author korend
 */
public class Conditions {
    
    public static Query createQuery(Condition... conditions) {
        return new Query().addCondition(and(conditions));
    }

    public static And and(Condition... conditions) {
        return new And(Arrays.asList(conditions));
    }

    public static Or or(Condition... conditions) {
        return new Or(Arrays.asList(conditions));
    }

    public static Equals eq(Attribute attr, Object value) {
        return new Equals(attr, value);
    }

    public static Greater gt(Attribute attr, Object value) {
        return new Greater(attr, value);
    }

    public static GreaterOrEquals gte(Attribute attr, Object value) {
        return new GreaterOrEquals(attr, value);
    }

    public static Lower lt(Attribute attr, Object value) {
        return new Lower(attr, value);
    }

    public static LowerOrEquals lte(Attribute attr, Object value) {
        return new LowerOrEquals(attr, value);
    }

    public static Not not(Condition... conditions) {
        return not(and(conditions));
    }

    public static Not not(Condition condition) {
        return new Not(condition);
    }

    public static NotEquals ne(Attribute attr, Object value) {
        return new NotEquals(attr, value);
    }

}
