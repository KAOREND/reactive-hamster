package com.kaibla.hamster.persistence.attribute;

import com.mongodb.BasicDBObject;
import com.kaibla.hamster.persistence.model.Document;
import static java.lang.Enum.valueOf;
import java.util.HashMap;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author kai
 */
public class EnumAttribute<T extends Enum<T>> extends Attribute {

    Class<T> enumType;
    HashMap<Object, String> enumMap = new HashMap<Object, String>();
    HashMap<String, T> ordinalMap = new HashMap<String, T>();
    T defaultValue;

    public EnumAttribute(Class table, String name, Class<T> enumType, T defaultValue) {
        super(table, name);
        this.defaultValue = defaultValue;
        this.enumType = enumType;
        for (T option : enumType.getEnumConstants()) {
            enumMap.put(valueOf(enumType, option.name()), option.name());
            ordinalMap.put(option.name(), option);
        }
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public Class<T> getEnumType() {
        return enumType;
    }

    @Override
    public int compare(Object o1, Object o2) {
        return getOrdinal(getName(o2)) - getOrdinal(getName(o1));
    }

    @Override
    public boolean equals(Object o1, Object o2) {
        return o1 == o2;
    }

    public int getOrdinal(String name) {
        T constant = ordinalMap.get(name);
        return constant.ordinal();
    }

    public T getValue(String name) {
        return valueOf(enumType, name);
    }

    public T get(Document o) {
        String s;
        if (shouldReadShadowCopy(o)) {
            s = o.getDataObject().getString(this.getShadowName());
        } else {
            s = o.getDataObject().getString(this.getName());
        }

        if (s != null) {
            return getValue(s);
        } else {
            return getDefaultValue();
        }
    }

    public Document set(Document o, T value) {
        o.valueChanged(this);
        o.getDataObject().put(this.getName(), getName(value));
        return o;
    }

    @Override
    public void set(org.bson.Document dataObject, Object value) {
        createShadowCopy(dataObject);
        Object o = getName(value);
        if (o != null) {
            dataObject.put(this.getName(), o);
        } else {
            dataObject.put(this.getName(), value);
        }
    }

    public T[] getOptions() {
        return enumType.getEnumConstants();
    }

    public String getName(Object o) {
        return enumMap.get(o);
    }
    private static final Logger LOG = getLogger(EnumAttribute.class.getName());
}
