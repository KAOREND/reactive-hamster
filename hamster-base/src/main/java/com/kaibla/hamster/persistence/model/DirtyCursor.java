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
         DocumentCursor cursor = new DocumentCursor(new WrappedQuery(query){
            @Override
            public Bson getQuery() {
               return query.getShadowQuery();
            }
            
        },dc);
        
        
        content = new BalancedTree<Document>(query);
        int i=0;
        for(Document doc: cursor) {
            if(query.isInQuery(doc) && doc.isVisible()) {                
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
