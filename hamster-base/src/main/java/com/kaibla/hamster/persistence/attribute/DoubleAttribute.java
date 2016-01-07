package com.kaibla.hamster.persistence.attribute;

import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class DoubleAttribute extends Attribute {

    public DoubleAttribute(Class table, String name) {
        super(table, name);
    }

    @Override
    public int compare(Object o1, Object o2) {
        Double d1 = (Double) o1;
        Double d2 = (Double) o2;
        return d1.compareTo(d2);
    }

    @Override
    public boolean equals(Object o1, Object o2) {
        Double d1 = (Double) o1;
        Double d2 = (Double) o2;
        return d1.doubleValue() == d2.doubleValue();
    }
    private static final Logger LOG = getLogger(DoubleAttribute.class.getName());
}
