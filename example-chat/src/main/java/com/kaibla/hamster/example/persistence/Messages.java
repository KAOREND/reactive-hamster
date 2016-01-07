package com.kaibla.hamster.example.persistence;

import com.kaibla.hamster.base.AbstractListenerOwner;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.base.UIContext;
import com.kaibla.hamster.persistence.attribute.DateAttribute;
import com.kaibla.hamster.persistence.attribute.DocumentReferenceAttribute;
import com.kaibla.hamster.persistence.attribute.StringAttribute;
import com.kaibla.hamster.persistence.model.DocumentCollection;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.QueryResultListModel;
import com.kaibla.hamster.persistence.query.Query;
import com.mongodb.DB;
import java.util.Date;

/**
 * This class defines the collection of messages. It both defines the attributes
 * of a message and contains all Data Operations (queries and write operations) for messages
 * 
 * @author korend
 */
public class Messages extends DocumentCollection {

    /**
     * The Text attribute of a message
     */
    public final static StringAttribute TEXT = new StringAttribute(Messages.class, "text");

    /**
     * The creation time attribute of a message
     */
    public final static DateAttribute CREATION_TIME = new DateAttribute(Messages.class, "creation_time");
    /**
     * A reference to the User, who has created this the message
     */
    public final static DocumentReferenceAttribute USER = new DocumentReferenceAttribute(Messages.class, "author", Users.class);
    
    
    private static final long serialVersionUID = 1L;

    public Messages(HamsterEngine engine, DB db) {
        // not much to do here, we just give this collection the name: messages
        super(engine, db, "messages");
        //if needed we would add the index creation here
    }

    /**
     * Creates a new Message and takes the necessary meta information from the UIContext
     * 
     * @param text Text of the new message
     */
    public void addMessage(String text) {
        Document<Messages> message = super.createNew(); //create empty document for the new message
        message.set(CREATION_TIME, new Date()); // set the current time as creation time
        message.set(USER, UIContext.getUser()); // set User who is doing this as creator of the message
        message.set(TEXT, text); //set the Text of the message
        message.writeToDatabase(); //writes the new document to the Database and fires events for it
    }
    
    /**
     * Returns the messages as list model. The list model is event driven and will fire events
     * whenever the content changes.
     * 
     * @param owner The owner of this list model, in our case this will be the List UI Component that will display the messages.
     * The owner is used to make sure that the list model will live as long as its owner is alive. 
     * 
     * @return List Model container the messages order by their creation time.
     */
    public QueryResultListModel getMessages(AbstractListenerOwner owner) {
        return query(owner,new Query().addOrder(CREATION_TIME, true));
    }
}
