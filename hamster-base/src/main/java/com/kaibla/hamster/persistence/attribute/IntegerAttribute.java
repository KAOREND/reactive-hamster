/*
 * 
 * .
 */
package com.kaibla.hamster.persistence.attribute;

import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class IntegerAttribute extends Attribute {
    int defaultValue;
    public IntegerAttribute(Class table, String name) {
        super(table, name);
    }
    
    public IntegerAttribute(Class table, String name,int defaultValue) {
        super(table, name);
        this.defaultValue = defaultValue;
    }

    @Override
    public int compare(Object o1, Object o2) {
        Integer i1 = (Integer) o1;
        Integer i2 = (Integer) o2;
        return i1.compareTo(i2);
    }

    public int getDefaultValue() {
        return defaultValue;
    }
    
    

    @Override
    public boolean equals(Object o1, Object o2) {
        Integer i1 = (Integer) o1;
        Integer i2 = (Integer) o2;
        return i1.intValue() == i2.intValue();
    }
    private static final Logger LOG = getLogger(IntegerAttribute.class.getName());
}
