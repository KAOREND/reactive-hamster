package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.DatabaseListModel;
import com.kaibla.hamster.persistence.model.DocumentCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.kaibla.hamster.base.AbstractListenerOwner;
import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.DataModel;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.events.DataObjectChangedEvent;
import com.kaibla.hamster.persistence.events.DataObjectCreatedEvent;
import com.kaibla.hamster.persistence.events.DataObjectDeletedEvent;
import com.kaibla.hamster.persistence.events.ListChangedEvent;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.query.BaseQuery;
import com.kaibla.hamster.collections.BalancedTree;
import com.mongodb.Block;
import com.mongodb.client.model.CountOptions;
import java.io.Serializable;
import java.util.SortedSet;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import org.bson.conversions.Bson;

/**
 *
 * @author kai
 */
public class QueryResultListModel extends DatabaseListModel implements Serializable {

    DocumentCollection table;
    QueryResultListModel self = this;

    public QueryResultListModel(DocumentCollection table, AbstractListenerOwner owner, DataModel model, BaseQuery query) {
        super(owner, model, query);
        this.table = table;
    }

    private transient BalancedTree<Document> cachedList = null;
    private transient long lastStartIndex = -1;
    private transient long lastElements = -1;

    private void initTreeSet() {
        cachedList = new BalancedTree<Document>(query);
    }

    @Override
    public long getSize() {
        long count = table.getCollection().count(query.getQuery());
        return count;
    }

    private void superFireChangedEvent(DataEvent e) {
        super.fireChangedEvent(e);
    }

    @Override
    public void fireChangedEvent(final DataEvent e) {
        getEngine().executeSynchronouslyIfInSamePage(new Runnable() {

            @Override
            public void run() {
                if (cachedList == null) {
                    initTreeSet();
                }
//      if(e instanceof DataObjectCreatedEvent && ((DataObjectCreatedEvent)e).getMongoObject().getTable() == Data.SearchIndex) {
//          LOG.info("created event on searchindex");          
//      }
                Document mo = (Document) e.getSource();
                if (query.isInQuery(mo)) {
                    if (e instanceof DataObjectCreatedEvent) {
                        addToCache(mo);
                        superFireChangedEvent(e);
                    } else if (e instanceof DataObjectDeletedEvent) {
                        cachedList.remove(mo);
                        superFireChangedEvent(e);
                    } else if (e instanceof DataObjectChangedEvent) {
                        if (cachedList.contains(mo)) {
                            DataObjectChangedEvent d = (DataObjectChangedEvent) e;
                            //update order
                            boolean orderChanged = false;
                            if (d.getChangedAttributes() == null) {
                                orderChanged = true;
                            } else {
                                for (Attribute changedAttribute : d.getChangedAttributes()) {
                                    if (query.isOrderAttribute(changedAttribute)) {
                                        orderChanged = true;
                                    }
                                }
                            }
//                    System.out.
//                            println("tableListModel test order changed: " + orderChanged + "   " + table.
//                            getCollectionName()+"  query: "+query+" mo "+mo);
                            if (orderChanged) {
                                cachedList.remove(mo);
                                addToCache(mo);
                                superFireChangedEvent(new ListChangedEvent(e.
                                        getSource(), e.getSource()));
                            }
                        } else {
                            addToCache(mo);
                            superFireChangedEvent(new ListChangedEvent(e.
                                    getSource(), e.getSource()));
                        }
                    }
                } else if (cachedList.contains(mo)) {
                    cachedList.remove(mo);
                    superFireChangedEvent(new DataObjectDeletedEvent(self, mo));
                }
            }
        }, getOwner().getListenerContainer());

    }

    public void addToCache(Document e) {
        if (cachedList == null) {
            initTreeSet();
        }
        cachedList.add(e);
        getOwner().holdDataModel(e);
    }

    public void removeFromCache(Document e) {
        if (cachedList != null) {
            cachedList.remove(e);
        }
        superFireChangedEvent(new DataObjectDeletedEvent(self, e));
    }

    @Override
    public boolean isDestroyed() {
        return super.isDestroyed() || !hasListeners();
    }

//    private boolean testIfOrderChanged(MongoObject mo) {
////        if (!cachedList.contains(mo)) {
////            return true;
////        }
//        MongoObject higher = cachedList.higher(mo);
//        if (higher != null && query.compare(mo, higher) == -1) {
//            return true;
//        }
//        MongoObject lower = cachedList.lower(mo);
//        if (lower != null && query.compare(mo, lower) == 1) {
//            return true;
//        }
//        return false;
//    }
    @Override
    public SortedSet get(long startIndex, long elements) {
        if (cachedList == null) {
            initTreeSet();
        }
        if (lastElements == elements && startIndex == lastStartIndex) {
            while (cachedList.size() > elements) {
                cachedList.pollLast();
            }
            return cachedList;
        }

        Document lastEntryTemp = null;
        if (startIndex == lastStartIndex + lastElements) {
            //we can avoid skips by creating a sort query using the lastEntry
            lastEntryTemp = cachedList.last();
        }
        final Document lastEntry = lastEntryTemp;
        initTreeSet();
        lastStartIndex = startIndex;
        lastElements = elements;
        if (lastEntry != null) {
             // avoid skips by creating a query from the lastEntry
              new ShadowAwareCursor(new WrappedQuery(query){
                  @Override
                  public Bson getQuery() {
                     return query.getSortQuery(lastEntry);
                  }
              } , table).setLimit((int) elements).forEeach(new Block<Document>() {
                @Override
                public void apply(Document t) {
                    addToCache(t);
                }
            });
        } else {
            new ShadowAwareCursor(query, table).setSkip((int) startIndex).setLimit((int) elements).forEeach(new Block<Document>() {
                @Override
                public void apply(Document t) {
                    addToCache(t);
                }
            });
        }
        return cachedList;
    }

    @Override
    public SortedSet get() {
        if (cachedList != null) {
            return cachedList;
        }
        initTreeSet();
        new ShadowAwareCursor(query, table).forEeach(new Block<Document>() {
            @Override
            public void apply(Document t) {
                addToCache(t);
            }
        });
        return cachedList;
    }

    @Override
    public boolean contains(Object o) {
        Document m = (Document) o;
        return query.isInQuery(m);
    }

    @Override
    public long getSize(int max) {
        CountOptions co = new CountOptions();
        co.limit(max);
        return table.getCollection().count(query.getQuery());
    }

    public Object prepareResume(HamsterEngine engine) {
        setParentModel(table);
        return this;
    }
    private static final Logger LOG = getLogger(QueryResultListModel.class.getName());

}
