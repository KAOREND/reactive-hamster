package com.kaibla.hamster.persistence.attribute;

import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class LongTextAttribute extends StringAttribute {

    public LongTextAttribute(Class table, String name) {
        super(table, name);
    }
    private static final Logger LOG = getLogger(LongTextAttribute.class.getName());
}
