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
public class LongAttribute extends Attribute {

    public LongAttribute(Class table, String name) {
        super(table, name);
    }

    @Override
    public int compare(Object o1, Object o2) {
        Long l1 = (Long) o1;
        Long l2 = (Long) o2;
        return l1.compareTo(l2);
    }

    @Override
    public boolean equals(Object o1, Object o2) {
        Long l1 = (Long) o1;
        Long l2 = (Long) o2;
        return l1.longValue() == l2.longValue();
    }
    private static final Logger LOG = getLogger(LongAttribute.class.getName());
}
