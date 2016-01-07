package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.attribute.Attribute;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class AttributeValue {

    private Attribute attr;
    private Object value;

    public AttributeValue(Attribute attr, Object value) {
        this.attr = attr;
        this.value = value;
    }

    /**
     * @return the attr
     */
    public Attribute getAttr() {
        return attr;
    }

    /**
     * @param attr the attr to set
     */
    public void setAttr(Attribute attr) {
        this.attr = attr;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }
    private static final Logger LOG = getLogger(AttributeValue.class.getName());
}
