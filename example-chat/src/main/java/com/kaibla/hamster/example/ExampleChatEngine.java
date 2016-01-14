package com.kaibla.hamster.example;

import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.base.HamsterSession;
import com.kaibla.hamster.base.UIEngine;
import com.kaibla.hamster.data.Users;
import com.kaibla.hamster.example.persistence.DocumentCollections;
import com.kaibla.hamster.example.ui.ExampleChatPage;
import com.kaibla.hamster.persistence.model.Document;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.gridfs.GridFS;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An Engine is the core of Hamster Framework App. From here everything else is
 * beeing initialized.
 *
 * @author korend
 */
public class ExampleChatEngine extends UIEngine {

    private static final long serialVersionUID = 1L;

    public ExampleChatEngine() {

    }

    @Override
    public synchronized void init() {
        super.init();
        //just init our persistence layer during Engine bootup
        initDB();
    }

    public void initDB() {
        //use the MongoDB "example-chat" for everything
        initDB("example-chat");
    }

    protected void initDB(String dbName) {
        //set up the persistence layer
        //Connect to the local MongoDB instance
        MongoClient m = new MongoClient();
        //get the DB with the given Name
        MongoDatabase chatDB = m.getDatabase(dbName);
        //initialize our collections
        DocumentCollections.init(this, chatDB);
        //set up GridFs for storing files
        GridFSBucket fs = GridFSBuckets.create(chatDB,"persistedPages");
            //the base class UIEngine needs the gridFS for
        //persisting sessions
        super.initDB(chatDB, fs);

    }

    @Override
    public HamsterPage createNewPage(HamsterSession session) {
        //this method is called by the engine when a new browser window is opend
        //by the user. We want to create a new instance of our ChatPage:
        return new ExampleChatPage(this, session);
    }

    @Override
    public Locale getUserLocale(Document user) {
        //This example only supports English
        return Locale.ENGLISH;
    }

    @Override
    public boolean userExists(String userid, String userhash) {
        //this example does not have a login mechanism, otherwise
        //we would check if the user credentials are valid here
        return true;
    }

    @Override
    public HamsterSession createSession(HttpServletRequest request, HttpServletResponse response, String sessionId) {
        //this is beinge called for creating a new session
        HamsterSession session = super.createSession(request, response, sessionId);
        //as we do not have login mask
        //in this simple example we just create a new user for each new session
        String namePrefix = "Unknown User";
        //find a name for the user which is still free:
        int i = 0;
        while (DocumentCollections.USERS.getUserByName(namePrefix + i) != null) {
            i++;
        }
        //create the new user
        Document user = DocumentCollections.USERS.createNew();
        user.set(Users.NAME, namePrefix + i);
        user.writeToDatabase();
        //login as the newly created user:
        session.setUser(user);
        return session;
    }
}
