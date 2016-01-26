
package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.query.BaseQuery;
import org.bson.conversions.Bson;

/**
 *
 * @author kai
 */
public class WrappedQuery implements BaseQuery {
    
    BaseQuery wrappedQuery;

    public WrappedQuery(BaseQuery wrappedQuery) {
        this.wrappedQuery = wrappedQuery;
    }

    @Override
    public Bson getQuery() {
        return wrappedQuery.getQuery();
    }

    @Override
    public Bson getShadowQuery() {
            return wrappedQuery.getShadowQuery();
        }

    @Override
    public Bson getSort() {
        return wrappedQuery.getSort();
    }

    @Override
    public Bson getSortQuery(Document startDoc) {
        return wrappedQuery.getSortQuery(startDoc);
    }

    @Override
    public boolean isInQuery(Document o) {
        return wrappedQuery.isInQuery(o);
    }

    @Override
    public int compare(Document o1, Document o2) {
        return wrappedQuery.compare(o1, o2);
    }

    @Override
    public boolean isOrderAttribute(Attribute attr) {
        return wrappedQuery.isOrderAttribute(attr);
    }

    @Override
    public int compare(Object o1, Object o2) {
        return wrappedQuery.compare(o1, o2);
    }
    
}
