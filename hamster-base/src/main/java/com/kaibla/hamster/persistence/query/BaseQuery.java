package com.kaibla.hamster.persistence.query;

import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.attribute.Attribute;
import java.io.Serializable;
import java.util.Comparator;
import org.bson.conversions.Bson;

/**
 *
 * @author kai
 */
public interface BaseQuery extends Comparator, Serializable {

    public abstract Bson getQuery();

    public abstract Bson getQueryPartOnly();

    public abstract boolean isInQuery(Document o);

    public abstract int compare(Document o1, Document o2);

    public abstract boolean isOrderAttribute(Attribute attr);
}
