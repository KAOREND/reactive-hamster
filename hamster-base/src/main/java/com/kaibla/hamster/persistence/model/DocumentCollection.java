package com.kaibla.hamster.persistence.model;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.kaibla.hamster.base.ChangedListener;
import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.DataModel;
import com.kaibla.hamster.base.AbstractListenerOwner;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.events.MongoEvent;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.query.Query;
import com.kaibla.hamster.persistence.query.BaseQuery;
import com.kaibla.hamster.persistence.attribute.LongTextAttribute;
import com.kaibla.hamster.persistence.attribute.SetAttribute;
import com.kaibla.hamster.persistence.query.Condition;
import com.kaibla.hamster.persistence.query.Equals;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import org.bson.types.ObjectId;

/**
 *
 * @author Kai Orend
 */
public abstract class DocumentCollection extends AttributeFilteredModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient ConcurrentHashMap<String, Document> map = null;
    private final transient DB db;
    private transient DBCollection collection;
    transient List<Attribute> attributes;
    transient DocumentCollection self = this;
    private static final HashMap<String, DocumentCollection> nameMap = new HashMap<String, DocumentCollection>();
    
    private static final HashMap<String, DocumentCollection> classMap = new HashMap<String, DocumentCollection>();


    private final HashMap<Attribute, FilteredModel> eventRoutings = new HashMap<Attribute, FilteredModel>();

    private final String tableName;

    public DocumentCollection(HamsterEngine engine, DB db, String name) {
        super(engine);
        map = new ConcurrentHashMap();
        this.db = db;
        this.tableName = name;
        collection = db.getCollection(name);
        attributes = new ArrayList();
        nameMap.put(name, this);
        classMap.put(this.getClass().getName(),this);
    }

    public synchronized void addChangedListener(ChangedListener listener, Query query) {
        boolean foundEqualsCondition = false;
        if (!query.getEventFilter().isEmpty()) {

            for (Equals equals : query.getEventFilter()) {
                if (!foundEqualsCondition) {
                    if (!(equals.getAttr() instanceof SetAttribute)) {
                        foundEqualsCondition = true;
                        addFilteredListener(equals.getAttr(), equals.getValue(), listener);
                        //only add one event route per query
                        break;
                    }
                }
            }
        } else {
            for (Condition condition : query.getConditions()) {
                if (condition instanceof Equals && !foundEqualsCondition) {
                    Equals equals = (Equals) condition;
                    if (!(equals.getAttr() instanceof SetAttribute)) {
                        foundEqualsCondition = true;
                        addFilteredListener(equals.getAttr(), equals.getValue(), listener);
                        //only add one event route per query
                        break;
                    }
                }
            }
        }
        if (!foundEqualsCondition) {
            LOG.warning("addChangeListener for Query: query did not contain an Equals condition that could be used for Event Routing");
            addChangedListener(listener);
        } else {
            if (listener instanceof AbstractListenerOwner) {
                AbstractListenerOwner comp = (AbstractListenerOwner) listener;
                comp.addEventFilter(new QueryFilter(self, query));
            }
        }
    }

    private void addFilteredListener(Attribute attr, Object value, ChangedListener listener) {
        FilteredModel filteredModel = eventRoutings.get(attr);
        if (filteredModel == null) {
            filteredModel = new FilteredModel(getEngine());
            eventRoutings.put(attr, filteredModel);
        }
        filteredModel.addChangedListener(listener, value);
    }

    @Override
    public synchronized boolean hasListeners() {
        return !eventRoutings.isEmpty() || super.hasListeners();
    }

    @Override
    public synchronized Collection getFilteredListener(DataEvent e) {
        if (e instanceof MongoEvent) {
            MongoEvent me = (MongoEvent) e;
            Document entity = me.getMongoObject();
            HashSet<ChangedListener> listeners = new HashSet<ChangedListener>();
            for (Attribute attr : eventRoutings.keySet()) {
                FilteredModel fm = eventRoutings.get(attr);
                Object o = entity.get(attr);
                if (o != null) {
                    Collection<ChangedListener> lis = fm.getFilteredListener(o);
                    if (lis != null) {
                        listeners.addAll(lis);
                    }
                }
            }
            listeners.addAll(super.getFilteredListener(e));
            return listeners;
        }
        return super.getFilteredListener(e);
    }

    @Override
    public synchronized void removeChangedListener(ChangedListener listener) {
        Iterator<FilteredModel> iter = eventRoutings.values().iterator();
        while (iter.hasNext()) {
            FilteredModel fm = iter.next();
            fm.removeChangedListener(listener);
            if (!fm.hasListeners()) {
                fm.destroy();
                iter.remove();
            }
        }
        super.removeChangedListener(listener);
    }

    public static DocumentCollection getByName(String name) {
        DocumentCollection t = nameMap.get(name);
        return t;
    }
    
    public static DocumentCollection getByClassName(String name) {
        DocumentCollection t = classMap.get(name);
        return t;
    }

    public Document createNew() {
        BasicDBObject newData = new BasicDBObject();
        ObjectId id = new ObjectId(new Date());
        newData.put("_id", id);
        Document newObject = new Document(getEngine(), this, newData);
        newObject.setNew(true);
        addToCache(newObject);
        return newObject;
    }

    public Document createNewDummy() {
        BasicDBObject newData = new BasicDBObject();
        ObjectId id = new ObjectId(new Date());
        newData.put("_id", id);
        Document newObject = new Document(getEngine(), this, newData);
        newObject.setNew(true);
        getEngine().removeModel(newObject);
        newObject.setIsDummy(true);
        return newObject;
    }

    public void addAttribute(Attribute attr) {
        attributes.add(attr);
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

    public String getTableName() {
        return tableName;
    }

    public void removeFromCache(Document obj) {
        synchronized (map) {
            if (isInCache(obj)) {
                obj.writeToDatabase(false);
                obj.setNew(false);
                map.remove(obj.getId());
                obj.destroy();
            }
        }
    }

    protected void destroyInCache(Document obj) {
        synchronized (map) {
            if (obj.getId() != null) {
                map.remove(obj.getId());
            }
        }
    }

    public void addToCache(Document obj) {
        synchronized (map) {
            Document existing = map.get(obj.getId());
            if (existing != null) {
                if (existing != obj) {
                    throw new IllegalStateException("detected entity which has two instance " + obj.getId() + " in  " + tableName);
                }
            } else {
                map.put(obj.getId(), obj);
            }
        }
        getEngine().addModel(this);
    }

    public boolean isInCache(Document obj) {
        synchronized (map) {
            Document t = map.get(obj.getId());
            return t == obj;
        }

    }

    public Document getEntityForMongo(BasicDBObject dbObject) {
        synchronized (map) {
            String id = "" + dbObject.get("_id");
            Document o = map.get(id);
            if (o == null) {
                o = new Document(getEngine(), this, dbObject);
                this.addToCache(o);
            }
            return o;
        }
    }

    public void writeToDatabase() {
        synchronized (map) {
            for (Document obj : map.values()) {
                obj.writeToDatabase();
                obj.setNew(false);
            }
        }
    }

    public Document getById(String id) {
        return getById(id, true, Context.getListenerContainer());
    }

    public Document getById(String id, AbstractListenerOwner owner) {
        return getById(id, true, owner);
    }

    /**
     * @param allowCache allows to access a database object without adding it to the cache. DO NOT EDIT non-cached
     * objects!
     */
    public Document getById(String id, boolean allowCache, ChangedListener owner) {
        if (id == null) {
            return null;
        }
        synchronized (map) {
            Document o = map.get(id);
            if (o != null) {
                // LOG.info("found sth in the cache");
                return o;
            }
            o = getByIdFromDatabase(id);
            if (allowCache && (o != null)) {
                addToCache(o);
            }
            if (o == null) {
                LOG.log(Level.INFO, "DatabaseObject not found: id: {0}", id);
                // Thread.dumpStack();
            }
            if (owner != null && o != null) {
                o.addHolder(owner);
            }
            return o;
        }

    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        Collection c = new LinkedList(map.values());
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            Document d = (Document) iter.next();
            synchronized (d) {
                if (!d.hasListeners() && !d.isNew()) {
                    removeFromCache(d);
                }
            }
        }
    }

    protected Document getByIdFromDatabase(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        BasicDBObject object = (BasicDBObject) collection.
                findOne(new BasicDBObject("_id", new ObjectId(id)));
        if (object == null) {
            return null;
        }
        return new Document(getEngine(), this, object);
    }

    /**
     * Fires an event on all objects in the cache, which fulfill the query
     *
     * @param event
     * @param query
     */
    public void fireEvent(DataEvent event, BaseQuery query) {
        //query only ids
        DBCursor cursor = collection.find(query.getQuery(), new BasicDBObject());
        while (cursor.hasNext()) {
            BasicDBObject dbObject = (BasicDBObject) cursor.next();
            String id = "" + dbObject.get("_id");
            synchronized (map) {
                Document o = map.get(id);
                if (o != null) {
                    o.fireChangedEvent(event);
                }
            }
        }
    }

    /**
     * This method should be overriden to fireEvents when an objects has been changed.
     *
     * @param e
     */
    public void fireEvents(DataEvent e) {
    }

    public void ensureIndex(boolean unique, boolean sparse, Attribute... attr) {
        BasicDBObject keys = new BasicDBObject();
        for (Attribute a : attr) {
            if (a instanceof LongTextAttribute) {
                keys.append(a.getName(), "text");
            } else {
                keys.append(a.getName(), 1);
            }
            LOG.log(Level.INFO, "ensured Index on {0}  {1}", new Object[]{getTableName(), a.
                getName()});
        }
        keys.append("unique", unique);
        if (unique) {
            keys.append("dropDups", true);
        }
        keys.append("sparse", sparse);
        collection.ensureIndex(keys);
        LOG.log(Level.INFO, "Indexes: {0}", collection.getIndexInfo());
    }

    public void dropAllIndexes() {
        collection.dropIndexes();
    }

    public Document queryOne(BaseQuery query) {
        BasicDBObject dbObject = (BasicDBObject) collection.findOne(query.
                getQuery());
        if (dbObject == null) {
            return null;
        }
        String id = "" + dbObject.get("_id");
        synchronized (map) {
            Document o = map.get(id);
            if (o == null) {
                o = new Document(getEngine(), self, dbObject);
                addToCache(o);
            }
            return o;
        }
    }

    public boolean exists(BaseQuery query) {
        BasicDBObject dbObject = (BasicDBObject) collection.findOne(query.
                getQuery());
        return dbObject != null;
    }

    public long count(BaseQuery query) {
        return collection.count(query.getQueryPartOnly());
    }

    public List<Document> query(BaseQuery query) {
        ArrayList list = new ArrayList(16);
        DBCursor cursor = collection.find(query.getQuery());
        while (cursor.hasNext()) {
            BasicDBObject dbObject = (BasicDBObject) cursor.next();
            String id = "" + dbObject.get("_id");
            synchronized (map) {
                Document o = map.get(id);
                if (o == null) {
                    o = new Document(getEngine(), self, dbObject);
                    addToCache(o);
                }
                if (query.isInQuery(o)) {
                    list.add(o);
                }
            }
        }
        return list;
    }

    public Iterable<Document> queryIterable(final BaseQuery query) {
        final DBCursor cursor = collection.find(query.getQuery());
        final Iterator<DBObject> iter = cursor.iterator();
        return new Iterable<Document>() {

            @Override
            public Iterator<Document> iterator() {
                return new Iterator<Document>() {
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public Document next() {
                        BasicDBObject dbObject = (BasicDBObject) iter.next();
                        String id = "" + dbObject.get("_id");
                        synchronized (map) {
                            Document o = map.get(id);
                            if (o == null) {
                                o = new Document(getEngine(), self, dbObject);
                                addToCache(o);
                            }
//                            if(query.isInQuery(o)) {
                                return o;
//                            } else {
//                                if(hasNext()) {
//                                    return next();
//                                } else {
//                                    return null;
//                                }
//                            }
                        }
                    }

                    @Override
                    public void remove() {
                        iter.remove();
                    }
                };
            }
        };
    }

//    public List<MongoObject> queryDistinct(Attribute distinctAttribute,BaseQuery query) {
//        ArrayList list = new ArrayList(16);
//        DBCursor cursor = collection.distinct(distinctAttribute.getName(), query.getQuery());
//        while (cursor.hasNext()) {
//            BasicDBObject dbObject = (BasicDBObject) cursor.next();
//            String id = "" + dbObject.get("_id");
//            MongoObject o = (MongoObject) map.get(id);
//            if (o == null) {
//                o = new MongoObject(getEngine(), self, dbObject);
//                addToCache(o);
//            }
//            list.add(o);
//        }
//        return list;
//    }
    public QueryResultListModel query(AbstractListenerOwner comp, BaseQuery query) {
        return query(comp, query, this);
    }

    private QueryResultListModel query(AbstractListenerOwner comp, BaseQuery query, DataModel eventObject) {
        return new QueryResultListModel(this, comp, eventObject, query);
    }

    /**
     * @return the db
     */
    public DB getDb() {
        return db;
    }

    /**
     * @return the collection
     */
    public DBCollection getCollection() {
        return collection;
    }

    public long getSize() {
        return collection.getCount();
    }

    public Object prepareResume(HamsterEngine engine) {
        return getByName(getTableName());
    }

    protected Object readResolve() {
//        LOG.info("MongoTable readResolve");
        return getByName(tableName);
    }

    protected Object writeReplace() {
//        LOG.info("MongoTable writeReplace");
        return new TablePlaceHolder(getTableName());
    }

    public void deleteContent() {
        cleanUp();
        collection.drop();
        collection = db.getCollection(tableName);
    }

    public static class TablePlaceHolder implements Serializable {

        String tableName;

        public TablePlaceHolder(String tableName) {
            this.tableName = tableName;
        }

        protected Object readResolve() throws ObjectStreamException {
            return getByName(tableName);
        }
    }
    private static final Logger LOG = getLogger(DocumentCollection.class.getName());
}