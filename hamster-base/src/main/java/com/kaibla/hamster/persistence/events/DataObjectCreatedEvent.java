/*
 * DatabaseObjectCreatedEvent.java Created on 25. Februar 2007, 13:54
 */
package com.kaibla.hamster.persistence.events;

import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.DataModel;
import com.kaibla.hamster.persistence.model.Document;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * @author Kai Orend
 */
public class DataObjectCreatedEvent extends DataEvent implements MongoEvent {

    private final Document databaseObject;

    /**
     * Creates a new instance of DatabaseObjectCreatedEvent
     */
    public DataObjectCreatedEvent(DataModel model, Document databaseObject) {
        super(model);
        this.databaseObject = databaseObject;
    }

    @Override
    public Document getMongoObject() {
        return databaseObject;
    }
    private static final Logger LOG = getLogger(DataObjectCreatedEvent.class.getName());

}
