/*
 * 
 * .
 */
package com.kaibla.hamster.persistence.attribute;

import com.kaibla.hamster.persistence.model.Document;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author kai
 */
public class SetAttribute extends Attribute {

    public SetAttribute(Class table, String name) {
        super(table, name);
    }

    @Override
    public int compare(Object o1, Object o2) {
        return 0;
    }

    @Override
    public boolean equals(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }

        HashSet set = (HashSet) o2;
        return set.contains(o1);
    }
    
    public HashSet get(Document doc) {
        ArrayList l;
        if (shouldReadShadowCopy(doc)) {
            l = (ArrayList) doc.getDataObject().get(getShadowName());
        } else {
            l = (ArrayList) doc.getDataObject().get(getName());
        }
        if (l == null) {
            return new HashSet();
        }
        return new HashSet(l);
    }

    @Override
    public void set(org.bson.Document dataObject, Object value) {
        createShadowCopy(dataObject);
        HashSet set = (HashSet) value;
        if (set == null) {
            dataObject.remove(getName());
        } else {
            dataObject.put(getName(), new ArrayList(set));
        }
    }
    private static final Logger LOG = getLogger(SetAttribute.class.getName());

}
