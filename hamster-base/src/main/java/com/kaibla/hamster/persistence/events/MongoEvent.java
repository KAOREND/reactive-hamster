package com.kaibla.hamster.persistence.events;

import com.kaibla.hamster.persistence.model.Document;

/**
 *
 * @author kai
 */
public interface MongoEvent {

    abstract Document getMongoObject();
}
