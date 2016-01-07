package com.kaibla.hamster.persistence.attribute;

import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class ObjectAttribute extends Attribute {

    public ObjectAttribute(Class table, String name) {
        super(table, name);
    }

    @Override
    public int compare(Object o1, Object o2) {
        return 0;
    }

    @Override
    public boolean equals(Object o1, Object o2) {
        return o1.equals(o2);
    }
    private static final Logger LOG = getLogger(ObjectAttribute.class.getName());
}
