package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.base.AbstractListenerOwner;
import com.kaibla.hamster.base.ChangedListener;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.events.DataObjectChangedEvent;
import com.kaibla.hamster.persistence.events.DataObjectCreatedEvent;
import com.kaibla.hamster.persistence.events.DataObjectDeletedEvent;
import com.kaibla.hamster.persistence.attribute.Attribute;
import static com.kaibla.hamster.persistence.model.DocumentCollection.getByName;
import com.kaibla.hamster.persistence.attribute.BooleanAttribute;
import com.kaibla.hamster.persistence.attribute.ComplexCopy;
import com.kaibla.hamster.persistence.attribute.DateAttribute;
import com.kaibla.hamster.persistence.attribute.DoubleAttribute;
import com.kaibla.hamster.persistence.attribute.DocumentReferenceAttribute;
import com.kaibla.hamster.persistence.attribute.FileAttribute;
import com.kaibla.hamster.persistence.attribute.IntegerAttribute;
import com.kaibla.hamster.persistence.attribute.LongAttribute;
import com.kaibla.hamster.persistence.attribute.ObjectAttribute;
import com.kaibla.hamster.persistence.attribute.SetAttribute;
import com.kaibla.hamster.persistence.attribute.StringAttribute;
import com.kaibla.hamster.collections.StringSource;
import com.mongodb.WriteConcern;
import static com.mongodb.client.model.Filters.*;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import org.bson.BsonInt32;
import org.bson.types.ObjectId;

/**
 *
 * @author Kai Orend
 */
public class Document<T extends DocumentCollection> extends AttributeFilteredModel implements Serializable {

    DocumentCollection collection;
    String id;
    private boolean isNew = false;
    transient org.bson.Document dataObject;
//    private transient boolean original=false;
    private final transient Set<Attribute> changedAttributes = Collections.newSetFromMap(new ConcurrentHashMap());
    private boolean isDummy = false;
    public final static String REVISION = "rev";

    public Document(HamsterEngine engine, DocumentCollection table, org.bson.Document dataObject) {
        super(engine);
//        original=true;
        assert dataObject != null;
        if (dataObject == null) {
            throw new IllegalArgumentException("dataObject must not be null");
        }
        this.dataObject = dataObject;
        this.collection = table;
//        tableName = collection.getCollectionName();
    }

    public void setIsDummy(boolean isDummy) {
        this.isDummy = isDummy;
    }

    public boolean isIsDummy() {
        return isDummy;
    }

    public DocumentCollection getCollection() {
        return collection;
    }

    public void valueChanged(Attribute attr) {
        if (changedAttributes.contains(attr)) {
            LOG.log(Level.INFO, "attribute changed twice in one transaction {0}", attr);
        }
        changedAttributes.add(attr);
    }

    public Set<Attribute> getChangedAttributes() {
        return changedAttributes;
    }

    @Override
    public synchronized void destroy() {
        super.destroy();
        collection.destroyInCache(this);
    }

    public StringSource getStringSource(final Attribute attr) {
        return new StringSource() {
            @Override
            public String toString() {
                return "" + get(attr);
            }
        };
    }

    public Object get(Attribute attr) {
        return attr.get(dataObject);
    }

    public Object get(ObjectAttribute attr) {
        return attr.get(dataObject);
    }

    public boolean get(BooleanAttribute attr) {
        return dataObject.getBoolean(attr.getName(), attr.getDefaultValue());
    }

    public Double get(DoubleAttribute attr) {
        return dataObject.getDouble(attr.getName());
    }

    public String get(StringAttribute attr) {
        String value = dataObject.getString(attr.getName());
        if (value != null && attr.isCaseInsensitive()) {
            return value.toLowerCase();
        }
        return value;
    }

    public long get(LongAttribute attr) {
        return dataObject.getLong(attr.getName());
    }

    public int get(IntegerAttribute attr) {
        try {
            return dataObject.getInteger(attr.getName(), attr.getDefaultValue());
        } catch (NullPointerException e) {
            return 0;
        }
    }

    public Document get(DocumentReferenceAttribute attr, AbstractListenerOwner owner) {
        return attr.getTable().getById(dataObject.getString(attr.getName()), owner);
    }

    public Document get(DocumentReferenceAttribute attr) {
        return attr.getTable().getById(dataObject.getString(attr.getName()));
    }

    public Date get(DateAttribute attr) {
        return (Date) dataObject.get(attr.getName());
    }

    public String get(FileAttribute attr) {
        return dataObject.getString(attr.getName());
    }

    public HashSet get(SetAttribute attr) {
        return attr.get(dataObject);
    }

    public Document set(Attribute attr, Object value) {
        Object oldValue = get(attr);
        if (oldValue != value || (value != null && !value.equals(oldValue))) {
            valueChanged(attr);
            attr.set(dataObject, value);
        }
        return this;
    }

    public Document set(ObjectAttribute attr, Object value) {
        Object oldValue = get(attr);
        if (oldValue != value || (value != null && value.equals(oldValue))) {
            valueChanged(attr);
            attr.set(dataObject, value);
        }
        return this;
    }

    public Document set(BooleanAttribute attr, boolean value) {
        if (!dataObject.containsKey(attr.getName()) || value != get(attr)) {
            valueChanged(attr);
            dataObject.put(attr.getName(), value);
        }
//        System.out.
//                println("mongoObject set boolean in: " + value + " out:" + get(attr) + " attr " + attr.
//                getName());
        return this;
    }

    public Document set(DoubleAttribute attr, double value) {
        valueChanged(attr);
        dataObject.put(attr.getName(), value);
        return this;
    }

    public Document set(StringAttribute attr, String value) {
        valueChanged(attr);
        if (value == null) {
            dataObject.remove(attr.getName());
        } else {
            if (attr.isCaseInsensitive()) {
                value = value.toLowerCase();
            }
            dataObject.put(attr.getName(), value);
        }
        return this;
    }

    public Document set(LongAttribute attr, long value) {
        valueChanged(attr);
        dataObject.put(attr.getName(), value);
        return this;
    }

    public Document set(IntegerAttribute attr, int value) {
        valueChanged(attr);
        dataObject.put(attr.getName(), value);
        return this;
    }

    public Document set(DocumentReferenceAttribute attr, Document value) {
        valueChanged(attr);
        if (value == null) {
            dataObject.remove(attr.getName());
        } else {
            if (value != null) {
                dataObject.put(attr.getName(), value.getId());
            }
        }
        return this;
    }

    public Document set(SetAttribute attr, HashSet set) {
        valueChanged(attr);
        attr.set(dataObject, set);
        return this;
    }

    public Document set(DateAttribute attr, Date value) {
        valueChanged(attr);
        if (value == null) {
            dataObject.remove(attr.getName());
        } else {
            dataObject.put(attr.getName(), value);
        }
        return this;
    }

    public boolean isFieldSet(Attribute attr) {
        return dataObject.containsKey(attr.getName());
    }

    public boolean isNew() {
        return isNew;
    }

    public synchronized void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    @Override
    public synchronized void removeChangedListener(ChangedListener listener) {
        super.removeChangedListener(listener);
        if (!hasListeners()) {
            // Objekt zerstören, wenn es keine Referenzen mehr darauf gibt
            destroy();
        }
    }

    public ObjectId getObjectId() {
        return dataObject.getObjectId("_id");
    }

    public String getId() {
        ObjectId id = getObjectId();
        if (id == null) {
            return null;
        }
        return id.toHexString();
    }

    public synchronized void writeToDatabase() {
        writeToDatabase(true);
    }

    public synchronized void writeToDatabase(boolean fireEvents) {
        writeToDatabase(fireEvents, WriteConcern.ACKNOWLEDGED);
    }

    public synchronized void writeToDatabase(boolean fireEvents, WriteConcern writeConcern) {
//      LOG.info("write to collection  "+collection.getCollection()+"  insert "+isNew);
//       LOG.info("       "+dataObject);        
        if (isNew) {
            dataObject.append(REVISION, 1);
            collection.getCollection().withWriteConcern(writeConcern).insertOne(dataObject);
            isNew = false;
            if (fireEvents) {
                DataObjectCreatedEvent event = new DataObjectCreatedEvent(this, this);
                this.fireChangedEvent(event);
                collection.fireChangedEvent(event);
                collection.fireEvents(event);
                getEngine().getEventQueue().pushEvent(event);
            }
            collection.addToCache(this);
        } else {
            collection.addToCache(this);
            if (dataObject.containsKey(REVISION)) {
                int oldRevision = dataObject.getInteger(REVISION);
                dataObject.put(REVISION, oldRevision + 1);
                Object old = collection.getCollection().withWriteConcern(writeConcern).
                        findOneAndReplace(
                                and(
                                        eq("_id", dataObject.get("_id")),
                                        eq(REVISION, new BsonInt32(oldRevision))
                                ), dataObject);
                if (old == null) {
                    //could not find orinal version, so our version of the document is stalled
                    throw new OptimisticLockException(this);
                }
            } else {
                //add revision to legacy documents
                dataObject.put(REVISION, 1);
                collection.getCollection().withWriteConcern(writeConcern).
                        replaceOne(eq("_id", dataObject.get("_id")), dataObject);
            }
            if (fireEvents) {
                fireChangedEvent();
            }
        }
        changedAttributes.clear();
//        LOG.info("write to database finsished");
    }

    public synchronized void addChangedAttribute(Attribute attr) {
        changedAttributes.add(attr);
    }

    public void fireChangedEvent() {
        DataObjectChangedEvent event = new DataObjectChangedEvent(this, new HashSet(changedAttributes));
        this.fireChangedEvent(event);
        collection.fireChangedEvent(event);
        collection.fireEvents(event);
        getEngine().getEventQueue().pushEvent(event);
    }

    public synchronized void delete() {
        collection.getCollection().deleteOne(dataObject);
        DataObjectDeletedEvent event = new DataObjectDeletedEvent(this, this);
        collection.fireChangedEvent(event);
        collection.fireEvents(event);
        this.fireChangedEvent(event);
        getEngine().getEventQueue().pushEvent(event);
        destroy();
    }

    public org.bson.Document getDataObject() {
        return dataObject;
    }

    public Document createClone() {
        org.bson.Document cloneData = collection.getByIdFromDatabase(getId()).getDataObject();
        Document clone = new Document(collection.getEngine(), collection, cloneData);
        return clone;
    }

    public Document createClone(Schema schema, Document user, boolean temp) {
        org.bson.Document cloneData = new org.bson.Document();
        Document clone = new Document(collection.getEngine(), collection, cloneData);
        clone.setIsDummy(temp);
        clone.merge(this, schema, user, temp);
        return clone;
    }

    public void merge(Document entity) {
        for (Iterator it = entity.getChangedAttributes().iterator(); it.hasNext();) {
            Attribute changedAttribute = (Attribute) it.next();
            this.set(changedAttribute, entity.get(changedAttribute));
        }
        dataObject.putAll(entity.getDataObject());
        this.changedAttributes.addAll(entity.getChangedAttributes());
    }

    public void merge(Document entity, Schema schema, Document user, boolean temp) {
        for (Attribute attr : schema.getAttributes()) {
            if (attr instanceof ComplexCopy) {
                ((ComplexCopy) attr).createCopy(entity, this, user, temp);
            } else {
                this.set(attr, entity.get(attr));
            }
        }
    }

//    public MongoObject getPreviousVersion() {
//        MongoObject p= new MongoObject(collection.getEngine(), collection, new org.bson.Document());
//        p.dataObject.putAll(dataObject.toMap());
//        for(Attribute attr : changedAttributes.keySet() ) {
//            p.set(attr, changedAttributes.get(attr));
//        }
//        return p;
//    }
//    public Resumeable prepareStore(HamsterEngine engine) {     
//        return new PlaceHolder(getId(),collection.getCollectionName());
//    }
//
//    public Object prepareResume(HamsterEngine engine) {
//        collection = DocumentCollection.getByName(tableName);
//        if(collection == null) {
//            System.err.println("collection is null "+tableName);
//        }
//        MongoObject mo = collection.getById(id);
//        if(!mo.original || mo.dataObject == null) {
//             System.err.println("collection contains non original mo "+tableName);
//        }
//        return mo;
//    }
//     private Object readResolve() throws ObjectStreamException  {
////        collection = DocumentCollection.getByName(tableName);
////        MongoObject mo = collection.getById(id);
////        if(!mo.original || mo.dataObject == null) {
////             System.err.println("collection contains non original mo "+tableName);
////        }
//        return collection.getById(id);
//    }
    public Object prepareResume(HamsterEngine engine) {
        return collection.getById(id);
    }

    protected Object writeReplace() {
//        if(collection.getByIdFromDatabase(getId()) == null) {
//            throw new IllegalStateException("cannot store mongobject that is not in db");
//        }
        return new PlaceHolder(getId(), collection.getCollectionName(), isDummy);
    }

    public static class PlaceHolder implements Serializable {

        String id;
        String tableName;
        boolean dummy;

        public PlaceHolder(String id, String tableName, boolean dummy) {
            this.id = id;
            this.tableName = tableName;
            this.dummy = dummy;
        }

        protected Object readResolve() throws ObjectStreamException {
            if (dummy) {
                return getByName(tableName).createNewDummy();
            }
            return getByName(tableName).getById(id);
        }
    }
    private static final Logger LOG = getLogger(Document.class.getName());
}
