package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.collections.BalancedTree;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.query.BaseQuery;
import java.util.Iterator;
import org.bson.conversions.Bson;

/**
 *
 * @author Kai
 */
public class DirtyCursor implements Iterable<Document> {

    BaseQuery query;
    DocumentCollection dc;
    int limit=-1;   
   
    BalancedTree<Document> content;
    public DirtyCursor(final BaseQuery query, DocumentCollection dc) {
        this.query = query;
        this.dc = dc;
         DocumentCursor cursor = new DocumentCursor(new BaseQuery() {
            @Override
            public Bson getQuery() {
               return query.getShadowQuery();
            }

            @Override
            public Bson getShadowQuery() {
                return query.getShadowQuery();
            }

            @Override
            public Bson getSort() {
                return query.getSort();
            }

            @Override
            public Bson getSortQuery(Document startDoc) {
                return query.getSortQuery(startDoc);
            }

            @Override
            public boolean isInQuery(Document o) {
                return query.isInQuery(o);
            }

            @Override
            public int compare(Document o1, Document o2) {
               return query.compare(o1, o2);
            }

            @Override
            public boolean isOrderAttribute(Attribute attr) {
               return query.isOrderAttribute(attr);
            }

            @Override
            public int compare(Object o1, Object o2) {
                return query.compare(o1, o2);
            }
        },dc);
        
        
        content = new BalancedTree<Document>(query);
        int i=0;
        for(Document doc: cursor) {
            if(query.isInQuery(doc)) {                
                i++;
                content.add(doc);
            }            
            if(limit != -1 ) {
                if(i >= limit) break;
            }
        }
        cursor.close();
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public Iterator<Document> iterator() {
        return content.iterator();
    }

}
