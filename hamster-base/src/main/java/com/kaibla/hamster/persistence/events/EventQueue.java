package com.kaibla.hamster.persistence.events;

import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.DocumentCollection;
import com.mongodb.Block;
import com.mongodb.CursorType;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import static com.mongodb.client.model.Filters.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import org.bson.types.ObjectId;

/**
 *
 * @author korend
 */
public class EventQueue {

    MongoDatabase db;
    MongoCollection queueCollection;
    HamsterEngine engine;
    String pid = "";

    private static final String COLLECTION_NAME = "eventQueueCapped";
    private static final long QUEUE_SIZE = 104857600;  // 100 MB

    private static final String TYPE = "type";

    private static final String CREATED = "created";

    private static final int TYPE_CHANGED = 1;
    private static final int TYPE_CREATED = 2;
    private static final int TYPE_DELETED = 3;

    private static final String DOCUMENT_ID = "doc_id";

    private static final String DOCUMENT_COLLECTION = "doc_col";

    private static final String DOCUMENT_REVISION = "doc_rev";

    private static final String DOCUMENT_OLD = "doc_old";

    private static final String CHANGED_ATTRIBUTES = "attributes";

    private static final String PID = "pid";

    ObjectId lastProcessed = null;

    public EventQueue(MongoDatabase db, HamsterEngine engine) {
        this.db = db;
        this.engine = engine;

        boolean exists = false;
        for (String name : db.listCollectionNames()) {
            if (name.equals(COLLECTION_NAME)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            CreateCollectionOptions options = new CreateCollectionOptions();
            options.capped(true);
            options.sizeInBytes(QUEUE_SIZE);
            options.maxDocuments(300);
            db.createCollection(COLLECTION_NAME, options);
        }
        queueCollection = db.getCollection(COLLECTION_NAME);
        pid = UUID.randomUUID().toString();

        Thread queuePoller = new Thread(new Runnable() {

            @Override
            public void run() {
                tailQueue();
            }
        }, "event queue processor");
        engine.addThread(queuePoller);
        queuePoller.start();
    }

    public String getPID() {
        return pid;
    }

    public void pushEvent(MongoEvent event) {
        org.bson.Document eventDocument = new org.bson.Document();
        Document doc = event.getMongoObject();
        eventDocument.put(PID, pid);
        eventDocument.put(CREATED, new Date().getTime());
        eventDocument.put(DOCUMENT_ID, doc.getObjectId());
        eventDocument.put(DOCUMENT_COLLECTION, doc.getCollection().getCollectionName());
        eventDocument.put(DOCUMENT_REVISION, doc.getDataObject().getInteger(Document.REVISION));
        if (event instanceof DataObjectCreatedEvent) {
            eventDocument.put(TYPE, TYPE_CREATED);
        } else if (event instanceof DataObjectDeletedEvent) {
            eventDocument.put(TYPE, TYPE_DELETED);
            eventDocument.put(DOCUMENT_OLD, doc.getDataObject());
        } else if (event instanceof DataObjectChangedEvent) {
            DataObjectChangedEvent changed = (DataObjectChangedEvent) event;
            eventDocument.put(TYPE, TYPE_CHANGED);
            ArrayList<String> attributeNames = new ArrayList(changed.getChangedAttributes().size());
            for (Attribute attr : changed.getChangedAttributes()) {
                attributeNames.add(attr.getCompleteName());
            }
            eventDocument.put(CHANGED_ATTRIBUTES, attributeNames);
        }
        queueCollection.insertOne(eventDocument);
    }

    private void tailQueue() {
        while (!engine.isDestroyed()) {
            try {
                FindIterable<org.bson.Document> cursor;
                if (lastProcessed == null) {
                    cursor = queueCollection.find(and(ne(PID, pid),gt(CREATED,new Date().getTime()))).cursorType(CursorType.TailableAwait);
                } else {
                    cursor = queueCollection.find(and(ne(PID, pid), gt("_id", lastProcessed))).cursorType(CursorType.TailableAwait);
                }
                cursor.forEach(new Block<org.bson.Document>() {
                    @Override
                    public void apply(final org.bson.Document event) {
                        lastProcessed = event.getObjectId("_id");
                        engine.execute(new Runnable() {

                            @Override
                            public void run() {
                                   processEvent(event);
                            }
                        }, null);
                     

                    }
                });
            } catch (Exception ex) {
                LOG.severe("Exception while tailing event queue: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void processEvent(org.bson.Document eventDocument) {
        int type = eventDocument.getInteger(TYPE);
        ObjectId documentId = eventDocument.getObjectId(DOCUMENT_ID);
        int revision = eventDocument.getInteger(DOCUMENT_REVISION);
        String collectionName = eventDocument.getString(DOCUMENT_COLLECTION);
        DocumentCollection collection = DocumentCollection.getByName(collectionName);
        Document doc = collection.reloadDocument(documentId.toHexString());
        if (doc != null && doc.getDataObject().getInteger(Document.REVISION) < revision) {
            LOG.severe("event queue is not consistent with document revision. Document revision event: "
                    + eventDocument.toJson() + " document in db: "
                    + doc.getDataObject().toJson());
        }
        DataEvent event = null;
        if (type == TYPE_CHANGED) {
            List<String> attributeNames = (List<String>) eventDocument.get(CHANGED_ATTRIBUTES);
            HashSet<Attribute> attributes = new HashSet();
            for (String name : attributeNames) {
                attributes.add(Attribute.getAttribute(name));
            }
            DataObjectChangedEvent c = new DataObjectChangedEvent(doc, attributes);
            event = c;
        } else if (type == TYPE_CREATED) {
            event = new DataObjectCreatedEvent(doc, doc);
        } else if (type == TYPE_DELETED) {
            if (doc == null) {
                org.bson.Document old = (org.bson.Document) eventDocument.get(DOCUMENT_OLD);
                doc = new Document(engine, collection, old);
            }
            event = new DataObjectDeletedEvent(doc, doc);
        }
        
        doc.fireChangedEvent(event);
        collection.fireChangedEvent(event);
        collection.fireEvents(event);
    }

    private static final Logger LOG = getLogger(EventQueue.class
            .getName());
}
