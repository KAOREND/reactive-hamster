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
public class StringAttribute extends Attribute {
    
    private boolean caseInsensitive=false;

    public StringAttribute(Class table, String name) {
        super(table, name);
    }
    
    public StringAttribute(Class table, String name,boolean caseInsensitive) {
        super(table, name);
        this.caseInsensitive=caseInsensitive;
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }   
    
    

    @Override
    public int compare(Object o1, Object o2) {
        String s1 = o1.toString();
        String s2 = o2.toString();
        return s1.compareTo(s2);
    }

    @Override
    public boolean equals(Object o1, Object o2) {
        String s1 = o1.toString();
        String s2 = o2.toString();
        return s1.equals(s2);
    }
    private static final Logger LOG = getLogger(StringAttribute.class.getName());
}
