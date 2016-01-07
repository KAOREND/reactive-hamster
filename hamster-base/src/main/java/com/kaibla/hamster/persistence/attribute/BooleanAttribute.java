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
public class BooleanAttribute extends Attribute {

    boolean defaultValue=false;
    public BooleanAttribute(Class table, String name) {
        super(table, name);
    }
    public BooleanAttribute(Class table, String name,boolean defaultValue) {
        super(table, name);
        this.defaultValue = defaultValue;
    }

    @Override
    public int compare(Object o1, Object o2) {
        Boolean a = (Boolean) o1;
        Boolean b = (Boolean) o2;
        return a.compareTo(b);
    }

    @Override
    public boolean equals(Object o1, Object o2) {
        Boolean a = (Boolean) o1;
        Boolean b = (Boolean) o2;
        return a.booleanValue() == b.booleanValue();
    }

    public void setDefaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public boolean getDefaultValue() {
        return defaultValue;
    }
          
    
    private static final Logger LOG = getLogger(BooleanAttribute.class.getName());
}
