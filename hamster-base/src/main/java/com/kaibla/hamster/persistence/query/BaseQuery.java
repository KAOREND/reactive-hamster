package com.kaibla.hamster.persistence.query;

import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.mongodb.BasicDBObject;
import java.io.Serializable;
import java.util.Comparator;

/**
 *
 * @author kai
 */
public interface BaseQuery extends Comparator, Serializable {

    public abstract BasicDBObject getQuery();

    public abstract BasicDBObject getQueryPartOnly();

    public abstract boolean isInQuery(Document o);

    public abstract int compare(Document o1, Document o2);

    public abstract boolean isOrderAttribute(Attribute attr);
}
