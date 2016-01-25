package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.query.BaseQuery;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import java.util.Iterator;

/**
 *
 * @author kai
 */
public class DocumentCursor implements Iterable<Document> {

    DocumentCollection dc;
    BaseQuery query;
    int limit = -1;
    int skip = -1;
    MongoCursor cursor = null;

    public DocumentCursor(BaseQuery query,DocumentCollection dc) {
        this.dc = dc;
        this.query = query;
    }

    public DocumentCursor setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public DocumentCursor setSkip(int skip) {
        this.skip = skip;
        return this;
    }

    public void close() {
        cursor.close();
    }

    @Override
    public Iterator<Document> iterator() {
        FindIterable fi = dc.getCollection().find(query.getQuery()).sort(query.getSort());
        if (limit != -1) {
            fi = fi.limit(limit);
        }
        if (skip != -1) {
            fi = fi.skip(skip);
        }

        cursor = fi.iterator();
        return new Iterator<Document>() {
            @Override
            public boolean hasNext() {
                return cursor.hasNext();
            }

            @Override
            public Document next() {
                org.bson.Document dbObject = (org.bson.Document) cursor.next();
                return dc.getDocumentForBSON(dbObject);
            }

            @Override
            public void remove() {
                cursor.remove();
            }
        };
    }
}
