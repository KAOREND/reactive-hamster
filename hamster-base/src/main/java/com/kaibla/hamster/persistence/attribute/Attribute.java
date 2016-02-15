package com.kaibla.hamster.persistence.attribute;

import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.persistence.model.Document;
import com.mongodb.BasicDBObject;
import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author kai
 */
public abstract class Attribute implements Serializable {

    private String name;
    boolean optional;
    private final Class collectionClass;
    private static HashMap<String, Attribute> attributeMap = new HashMap();
    private String completeName;

    public Attribute(Class collectionClass, String name) {
        this.name = name;
        this.collectionClass = collectionClass;
        completeName = collectionClass.getName() + name;
        if (attributeMap.containsKey(completeName)) {
            throw new RuntimeException("duplicate attribute instantiated " + completeName);
        }
        attributeMap.put(completeName, this);
    }
    
    public static void resetAttributeChecker() {
        attributeMap.clear();
    }

    public String getCompleteName() {
        return completeName;
    }

    public static Attribute getAttribute(String completeName) {
        return attributeMap.get(completeName);
    }

    public Class getCollectionClass() {
        return collectionClass;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public String getShadowName() {
        return "_" + getName();
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public abstract int compare(Object o1, Object o2);

    public abstract boolean equals(Object o1, Object o2);

    public Object get(Document doc) {
        if(shouldReadShadowCopy(doc)) {
           return doc.getDataObject().get(this.getShadowName()); 
        } else {
           return doc.getDataObject().get(this.getName()); 
        }
        
    }
    
    protected boolean shouldReadShadowCopy(Document doc) {
        return doc.shouldReadShadowCopy() && doc.getDataObject().containsKey(this.getShadowName());
    }

    public void createShadowCopy(org.bson.Document dataObject) {
        if (Context.getTransaction() != null) {
            if(!dataObject.containsKey(getShadowName())) { //never overwrite already existing shadow copies
                dataObject.put(getShadowName(), dataObject.get(this.getName()));
            }
        }
    }

    public void deleteShadowCopy(org.bson.Document dataObject) {
        dataObject.remove(getShadowName());
    }

    public void revertChanges(org.bson.Document dataObject) {
        dataObject.put(getName(), dataObject.get(this.getShadowName()));
        deleteShadowCopy(dataObject);
    }

    public void set(org.bson.Document dataObject, Object value) {
        createShadowCopy(dataObject);
        dataObject.put(getName(), value);
    }
}
