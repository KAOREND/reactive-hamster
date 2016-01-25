package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.query.BaseQuery;
import java.util.Iterator;
import org.bson.conversions.Bson;

/**
 *
 * @author kai
 */
public class ExtendableCursor implements Iterable<Document> {

    DocumentCollection dc;
    BaseQuery query;
    int blockSize = -1;
    int skip = -1;

    DocumentCursor cursor;
    Iterator<Document> docIter;

    public ExtendableCursor(DocumentCollection dc, BaseQuery query) {
        this.dc = dc;
        this.query = query;
    }

    public void setBlockSize(int limit) {
        this.blockSize = limit;
    }

    public void setSkip(int skip) {
        this.skip = skip;
    }

    @Override
    public Iterator<Document> iterator() {

        cursor = new DocumentCursor(query, dc);
        cursor.setLimit(blockSize);
        cursor.setSkip(skip);
        docIter = cursor.iterator();

        return new Iterator<Document>() {
            @Override
            public boolean hasNext() {
                return docIter.hasNext();
            }

            private void prepareNext(final Document previous) {
                if (!docIter.hasNext()) {
                    cursor.close();
                    if (previous != null) {
                        cursor.close();
                        cursor = new DocumentCursor(new BaseQuery() {
                            @Override
                            public Bson getQuery() {
                                return query.getSortQuery(previous);
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
                        }, dc);
                        docIter = cursor.iterator();
                    }
                }
            }

            @Override
            public Document next() {
                Document result = docIter.next();
                prepareNext(result);
                return result;

            }

            @Override
            public void remove() {
                docIter.remove();
            }
        };
    }

    public void close() {
        if (cursor != null) {
            cursor.close();
        }
    }

}
