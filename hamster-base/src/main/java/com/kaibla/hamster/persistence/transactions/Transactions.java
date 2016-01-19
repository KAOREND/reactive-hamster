package com.kaibla.hamster.persistence.transactions;

import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.model.DocumentCollection;
import com.mongodb.client.MongoDatabase;

/**
 *
 * @author korend
 */
public class Transactions extends DocumentCollection {
    
    public Transactions(HamsterEngine engine, MongoDatabase db, String name) {
        super(engine, db, name);
    }
    
}
