/*
 */
package com.kaibla.hamster.example.persistence;

import com.kaibla.hamster.base.UIEngine;
import com.kaibla.hamster.persistence.query.Query;
import com.mongodb.DB;

/**
 * This collections contains all our users. As the Hamster UI Engine already has a basic concept of users,
 * this class extends com.kaibla.hamster.data.Users
 * 
 * @author korend
 */
public class Users extends com.kaibla.hamster.data.Users {
    
    private static final long serialVersionUID = 1L;

    public Users(UIEngine engine, DB db) {
        //our users will be stored in the MongoDB collection "users"
        super(engine, db, "users");
    }
    
    /**
     * Verifies that a users password is correct.
     * 
     * @param userName  Name of the user
     * @param hashedPassword md5 hashed user password
     * @return true if a user with the hashed Password and the given user name exists 
     */
    public boolean verifyUser(String userName,String hashedPassword) {
        return exists(new Query().equals(NAME, userName).equals(PASSWORD, hashedPassword));
    }
    
}
