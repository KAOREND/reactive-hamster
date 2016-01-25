package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.query.BaseQuery;
import com.mongodb.client.model.Filters;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.bson.conversions.Bson;

/**
 *
 * @author kai
 */
public class ShadowAwareCursor implements Iterable<Document> {

    BaseQuery query;
    DocumentCollection dc;
    int limit = -1;
    int skip = -1;
    DirtyCursor shadowCursor;
    DocumentCursor documentCursor;

    public ShadowAwareCursor(final BaseQuery query, DocumentCollection dc) {
        this.query = query;
        this.dc = dc;
        shadowCursor = new DirtyCursor(query, dc);
        documentCursor = new DocumentCursor(new BaseQuery() {
            @Override
            public Bson getQuery() {
               return Filters.and(query.getQuery(),Filters.not(Filters.exists(Document.DIRTY)));
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
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public Iterator<Document> iterator() {
        return new Iterator<Document>() {

            Document next = null;
            boolean started = false;

            Document nextClean = null;
            Document nextDirty = null;
            Iterator<Document> sIter = shadowCursor.iterator();
            Iterator<Document> cIter = documentCursor.iterator();

            @Override
            public boolean hasNext() {
                if (!started) {
                    fetchNext(null);
                }
                return nextClean != null || nextDirty != null;
            }

            @Override
            public Document next() {
                if (!started) {
                    fetchNext(null);
                }
                if(nextClean == null && nextDirty == null) {
                    throw new NoSuchElementException();
                }
                Document result = null;
                if(nextClean != null && nextDirty == null) {                    
                    result=nextClean;
                    nextClean=null;
                } else if(nextClean == null && nextDirty != null) {
                    result = nextDirty;
                    nextDirty = null;
                } else if(nextClean == nextDirty && nextClean != null) {
                    result = nextClean;
                    nextClean = null;
                    nextDirty = null;
                } else if(query.compare(nextClean, nextDirty) < 0) {
                    result = nextClean;
                    nextClean=null;
                } else {
                   result = nextDirty;
                   nextDirty = null;
                }
                fetchNext(result);
                return result;
            }

            private void fetchNext(Document last) {
                started = true;
                if (sIter.hasNext() && nextDirty == null) {
                    nextDirty = sIter.next();                    
                }
                if (cIter.hasNext() && nextClean == null) {
                    nextClean = cIter.next();
                }                
                if(last != null) {
                    if(last == nextDirty) {
                        nextDirty = null; 
                        fetchNext(last);
                    } else if(last == nextClean) {
                        nextClean=null;
                        fetchNext(last);
                    }
                }
            }
        };
    }

    public void close() {
        documentCursor.close();
    }

}
