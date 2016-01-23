package com.kaibla.hamster.persistence.transactions;

import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.attribute.DateAttribute;
import com.kaibla.hamster.persistence.attribute.EnumAttribute;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.DocumentCollection;
import com.mongodb.client.MongoDatabase;
import java.util.Date;

/**
 *
 * @author korend
 */
public class Transactions extends DocumentCollection {

    public final static DateAttribute START = new DateAttribute(Transactions.class, "start");
    
    public final static DateAttribute END = new DateAttribute(Transactions.class, "end");

    public enum State {
        STARTED, COMMTTING, COMMITTED, ROLLING_BACK, ROLLED_BACK, NOT_EXISTENT;
    }

    public final static EnumAttribute<State> STATE = new EnumAttribute<State>(Transactions.class, "state", State.class, State.STARTED);

    private static final long serialVersionUID = 1L;

    public Transactions(HamsterEngine engine, MongoDatabase db) {
        super(engine, db, "transactions");
    }

    @Override
    public Document createNew() {
        Document doc=super.createNew();
        doc.set(START, new Date());
        return doc;
    }
    
    

}
