package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.query.BaseQuery;
import com.mongodb.Block;
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
    boolean initialized = false;

    public ShadowAwareCursor(final BaseQuery query, DocumentCollection dc) {
        this.query = query;
        this.dc = dc;
        shadowCursor = new DirtyCursor(query, dc);
        documentCursor = new DocumentCursor(new WrappedQuery(query){
            @Override
            public Bson getQuery() {
                return Filters.and(query.getQuery(), Filters.not(Filters.exists(Document.DIRTY)));
            }
        }, dc);
    }

    private void init(Iterator<Document> iter) {
        initialized = true;
        
        if (skip != -1 && limit != -1) {
            limit += skip;
        }
        if (limit != -1) {
            documentCursor.setLimit(limit);
            shadowCursor.setLimit(limit);
        }
        if (skip != -1) {
            //because of possible shadow copies we cannot skip on the db
            int i = 0;
            while (i < skip && iter.hasNext()) {
                i++;
                iter.next();
            }
        }
    }

    public ShadowAwareCursor setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public ShadowAwareCursor setSkip(int skip) {
        this.skip = skip;
        return this;
    }

    public void forEeach(Block<Document> block) {
        try {
            for (Document doc : this) {
                block.apply(doc);
            }
        } finally {
            close();
        }
    }

    @Override
    public Iterator<Document> iterator() {
        Iterator<Document> iter = new Iterator<Document>() {

            Document next = null;
            boolean started = false;

            Document nextClean = null;
            Document nextDirty = null;
            Iterator<Document> sIter = shadowCursor.iterator();
            Iterator<Document> cIter = documentCursor.iterator();
            long counter=0;
            @Override
            public boolean hasNext() {
                if (!started) {
                    fetchNext(null);
                }
                if(limit != -1) {
                    if(counter >= limit) {
                        return false;
                    }
                }
                return nextClean != null || nextDirty != null;
            }

            @Override
            public Document next() {
                counter++;
                if (!started) {
                    fetchNext(null);
                }
                if (nextClean == null && nextDirty == null) {
                    throw new NoSuchElementException();
                }
                Document result = null;
                if (nextClean != null && nextDirty == null) {
                    result = nextClean;
                    nextClean = null;
                } else if (nextClean == null && nextDirty != null) {
                    result = nextDirty;
                    nextDirty = null;
                } else if (nextClean == nextDirty && nextClean != null) {
                    result = nextClean;
                    nextClean = null;
                    nextDirty = null;
                } else if (query.compare(nextClean, nextDirty) < 0) {
                    result = nextClean;
                    nextClean = null;
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
                if (last != null) {
                    if (last == nextDirty) {
                        nextDirty = null;
                        fetchNext(last);
                    } else if (last == nextClean) {
                        nextClean = null;
                        fetchNext(last);
                    }
                }
            }
        };
        init(iter);
        return iter;
    }

    public void close() {
        documentCursor.close();
    }

}
