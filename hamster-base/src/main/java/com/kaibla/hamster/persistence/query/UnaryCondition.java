package com.kaibla.hamster.persistence.query;

import com.kaibla.hamster.persistence.attribute.Attribute;

/**
 *
 * @author kai
 */
public abstract class UnaryCondition extends Condition {

    Attribute attr;
    Object value;

    public UnaryCondition(Attribute attr, Object value) {
        this.attr = attr;
        this.value = value;
    }

    public Attribute getAttr() {
        return attr;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof UnaryCondition) {
            UnaryCondition uc = (UnaryCondition) o;
            return uc.attr == attr && value.equals(uc.value);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hashCode=1;        
        hashCode = 31 * hashCode +attr.hashCode();
        hashCode = 31 * hashCode +value.hashCode();
        return hashCode; 
    }

}
