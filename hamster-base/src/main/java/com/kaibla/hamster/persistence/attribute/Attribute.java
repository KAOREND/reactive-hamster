package com.kaibla.hamster.persistence.attribute;

import com.mongodb.BasicDBObject;
import java.io.Serializable;

/**
 *
 * @author kai
 */
public abstract class Attribute implements Serializable {

    private String name;
    boolean optional;
    private final Class tableClass;

    public Attribute(Class tableClass, String name) {
        this.name = name;
        this.tableClass = tableClass;
    }

    public Class getTableClass() {
        return tableClass;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public abstract int compare(Object o1, Object o2);

    public abstract boolean equals(Object o1, Object o2);

    public Object get(org.bson.Document dataObject) {
        return dataObject.get(this.getName());
    }

    public void set(org.bson.Document dataObject, Object value) {
        dataObject.put(getName(), value);
    }
}
