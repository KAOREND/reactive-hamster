package com.kaibla.hamster.example.persistence;

import com.kaibla.hamster.base.UIEngine;
import com.mongodb.client.MongoDatabase;

/**
 * This class is beeing used to initialize our Collections and make them accessable.
 * 
 * @author korend
 */
public class DocumentCollections {
    /**
     * The Messages collection contains all our messages
     */
    public static Messages MESSAGES = null;
    
    /**
     * Contains our Users
     */
    public static Users USERS = null;
    
    /**
     * Initializes the connections
     * @param engine The UI Engine
     * @param db A MongoDB
     */
    public static void init(UIEngine engine,MongoDatabase db) {
        MESSAGES = new Messages(engine,db);
        USERS = new Users(engine,db);
    }

}
