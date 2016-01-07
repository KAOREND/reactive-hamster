/*
 * DatabaseObjectCreatedEvent.java Created on 25. Februar 2007, 13:54
 */
package com.kaibla.hamster.persistence.events;

import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.model.Document;
import java.util.HashSet;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * @author Kai Orend
 */
public class DataObjectChangedEvent extends DataEvent implements MongoEvent {

    private final Document databaseObject;
    private final HashSet<Attribute> changedAttributes;

    /**
     * Creates a new instance of DatabaseObjectCreatedEvent
     */
    public DataObjectChangedEvent(Document databaseObject, HashSet<Attribute> changedAttributes) {
        super(databaseObject);
        this.databaseObject = databaseObject;
        this.changedAttributes = changedAttributes;
    }
    
    

    @Override
    public Document getMongoObject() {
        return databaseObject;
    }

    public HashSet<Attribute> getChangedAttributes() {
        return changedAttributes;
    }
    private static final Logger LOG = getLogger(DataObjectChangedEvent.class.getName());
}
