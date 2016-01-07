package com.kaibla.hamster.persistence.query;

import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.model.Document;
import java.io.Serializable;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author kai
 */
public class Order implements Serializable {

    Attribute attr;
    boolean descending;

    public Order(Attribute attr, boolean descending) {
        this.attr = attr;
        this.descending = descending;
    }

    public int compare(Document m1, Document m2) {
        Object o1;
        Object o2;
        if (!descending) {
            o1 = m1.get(attr);
            o2 = m2.get(attr);
        } else {
            o1 = m2.get(attr);
            o2 = m1.get(attr);
        }
        if (o1 == null && o2 == null) {
            //ensure always a defined order
            return m1.getId().compareTo(m2.getId());
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        int result = attr.compare(o1, o2);
        return result;
    }

    public Attribute getAttribute() {
        return attr;
    }

    public boolean isDescending() {
        return descending;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Order) {
            Order order = (Order) o;
            return order.descending == descending && order.attr == attr;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + attr.hashCode();
        if (descending) {
            hashCode = 1 + 31 * hashCode;
        }
        return hashCode;
    }

    private static final Logger LOG = getLogger(Order.class.getName());

}
