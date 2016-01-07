package com.kaibla.hamster.persistence.attribute;

import java.util.Date;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class DateAttribute extends Attribute {

    public DateAttribute(Class table, String name) {
        super(table, name);
    }

    @Override
    public int compare(Object o1, Object o2) {
        Date l1 = (Date) o1;
        Date l2 = (Date) o2;
        return l1.compareTo(l2);
    }

    @Override
    public boolean equals(Object o1, Object o2) {
        Date l1 = (Date) o1;
        Date l2 = (Date) o2;
        return l1.getTime() == l2.getTime();
    }
    private static final Logger LOG = getLogger(DateAttribute.class.getName());
}
