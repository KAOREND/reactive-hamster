package com.kaibla.hamster.persistence.model;

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
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
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
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public abstract class DocumentCollection extends AttributeFilteredModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient ConcurrentHashMap<String, Document> map = null;
    private final transient MongoDatabase db;
    private transient MongoCollection collection;
    transient List<Attribute> attributes;
    transient DocumentCollection self = this;
    private static final HashMap<String, DocumentCollection> nameMap = new HashMap<String, DocumentCollection>();

    private static final HashMap<String, DocumentCollection> classMap = new HashMap<String, DocumentCollection>();

    private final HashMap<Attribute, FilteredModel> eventRoutings = new HashMap<Attribute, FilteredModel>();

    private final String tableName;

    public DocumentCollection(HamsterEngine engine, MongoDatabase db, String name) {
        super(engine);
        map = new ConcurrentHashMap();
        this.db = db;
        this.tableName = name;
        collection = db.getCollection(name);
        attributes = new ArrayList();
        nameMap.put(name, this);
        classMap.put(this.getClass().getName(), this);

        org.bson.Document dirtyIndex = new org.bson.Document();
        dirtyIndex.append(Document.DIRTY, 1);
        dirtyIndex.append("unique", false);
        dirtyIndex.append("sparse", true);
        collection.createIndex(dirtyIndex);
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
        } else if (listener instanceof AbstractListenerOwner) {
            AbstractListenerOwner comp = (AbstractListenerOwner) listener;
            comp.addEventFilter(new QueryFilter(self, query));
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
        org.bson.Document newData = new org.bson.Document();
        ObjectId id = new ObjectId(new Date());
        newData.put("_id", id);
        Document newObject = new Document(getEngine(), this, newData);
        newObject.setNew(true);
        if (Context.getTransaction() != null) {
            //make sure that the new document is alive as long as the transaction runs
            newObject.addHolder(Context.getTransaction());
        }
        addToCache(newObject);
        return newObject;
    }

    public Document createNewDummy() {
        org.bson.Document newData = new org.bson.Document();
        ObjectId id = new ObjectId(new Date());
        newData.put("_id", id);
        Document newObject = new Document(getEngine(), this, newData);
        newObject.setNew(true);
        getEngine().removeModel(newObject);
        newObject.setIsDummy(true);
        if (Context.getTransaction() != null) {
            //make sure that the new document is alive as long as the transaction runs
            newObject.addHolder(Context.getTransaction());
        }
        addToCache(newObject);
        return newObject;
    }

    public void addAttribute(Attribute attr) {
        attributes.add(attr);
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

    public String getCollectionName() {
        return tableName;
    }

    public void removeFromCache(Document obj) {
        synchronized (map) {
            if (isInCache(obj)) {
                try {
                    obj.writeToDatabase(false);
                } catch (OptimisticLockException e) {
                    LOG.warning(e.getMessage());
                }
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
                    throw new IllegalStateException("detected entity which has two instances " + obj.getId() + " in  " + tableName);
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

    public boolean isInCache(String id) {
        synchronized (map) {
            return map.contains(id);
        }
    }

    public Document getEntityForMongo(org.bson.Document dbObject) {
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
        if (Context.getTransaction() != null) {
            return getById(id, true, Context.getTransaction());
        } else {
            return getById(id, true, Context.getListenerContainer());
        }
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
            if (d != null) {
                synchronized (d) {
                    if (!d.hasListeners() && !d.isNew()) {
                        removeFromCache(d);
                    }
                }
            }
        }
    }

    protected Document getByIdFromDatabase(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        org.bson.Document object = (org.bson.Document) collection.
                find(new org.bson.Document("_id", new ObjectId(id))).limit(1).first();
        if (object == null) {
            return null;
        }
        return new Document(getEngine(), this, object);
    }

    public Document reloadDocument(String id) {
        synchronized (map) {
            Document doc = map.get(id);
            if (doc != null) {
                org.bson.Document object = (org.bson.Document) collection.
                        find(new org.bson.Document("_id", new ObjectId(id))).limit(1).first();
                doc.setDataObject(object);
                return doc;
            } else {
                return getById(id);
            }
        }
    }

    public Document getDocumentForBSON(org.bson.Document dbObject) {
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

    /**
     * Fires an event on all objects in the cache, which fulfill the query
     *
     * @param event
     * @param query
     */
    public void fireEvent(final DataEvent event, BaseQuery query) {
        //query only ids
        FindIterable cursor = collection.find(query.getQuery());
        cursor.forEach(new Block() {

            @Override
            public void apply(Object t) {
                org.bson.Document dbObject = (org.bson.Document) t;
                String id = "" + dbObject.get("_id");
                synchronized (map) {
                    Document o = map.get(id);
                    if (o != null) {
                        o.fireChangedEvent(event);
                    }
                }
            }
        });
    }

    /**
     * This method should be overriden to fireEvents when an objects has been changed.
     *
     * @param e
     */
    public void fireEvents(DataEvent e) {
    }

    public void ensureIndex(boolean unique, boolean sparse, Attribute... attr) {
        org.bson.Document keys = new org.bson.Document();
        boolean textIndex=false;
        for (Attribute a : attr) {
            if (a instanceof LongTextAttribute) {
                keys.append(a.getName(), "text");
                textIndex=true;
            } else {
                keys.append(a.getName(), 1);
            }
            LOG.log(Level.INFO, "ensured Index on {0}  {1}", new Object[]{getCollectionName(), a.
                getName()});
        }
        if(!textIndex) {
            keys.append("unique", unique);
            keys.append("sparse", sparse);
        }
        collection.createIndex(keys);
    }

    public void dropAllIndexes() {
        collection.dropIndexes();
    }

    public Document queryOne(BaseQuery query) {
        ShadowAwareCursor c = new ShadowAwareCursor(query, this).setLimit(1);
        Iterator<Document> iter = c.iterator();
        try {
            if (iter.hasNext()) {
                return iter.next();
            } else {
                return null;
            }
        } finally {
            c.close();
        }
    }

    public boolean exists(BaseQuery query) {
        return queryOne(query) != null;
    }

    public long count(BaseQuery query) {
        return collection.count(query.getQuery());
    }

    public List<Document> query(final BaseQuery query) {
        final ArrayList list = new ArrayList(16);
        new ShadowAwareCursor(query, this).forEeach(new Block<Document>() {
            @Override
            public void apply(Document t) {
                list.add(t);
            }
        });
        return list;
    }

    public void forEach(final BaseQuery query, final Block<Document> block) {
        new ShadowAwareCursor(query, this).forEeach(block);
    }

    public Iterable<Document> queryIterable(final BaseQuery query) {
        return new ShadowAwareCursor(query, this);
    }

    public QueryResultListModel query(AbstractListenerOwner comp, BaseQuery query) {
        return query(comp, query, this);
    }

    private QueryResultListModel query(AbstractListenerOwner comp, BaseQuery query, DataModel eventObject) {
        return new QueryResultListModel(this, comp, eventObject, query);
    }

    /**
     * @return the db
     */
    public MongoDatabase getDb() {
        return db;
    }

    /**
     * @return the collection
     */
    public MongoCollection getCollection() {
        return collection;
    }

    public long getSize() {
        return collection.count();
    }

    public Object prepareResume(HamsterEngine engine) {
        return getByName(getCollectionName());
    }

    protected Object readResolve() {
//        LOG.info("MongoTable readResolve");
        return getByName(tableName);
    }

    protected Object writeReplace() {
//        LOG.info("MongoTable writeReplace");
        return new TablePlaceHolder(getCollectionName());
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
