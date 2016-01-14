package com.kaibla.hamster.data;

import com.mongodb.DB;
import com.kaibla.hamster.base.ChangedListener;
import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.UIEngine;
import com.kaibla.hamster.persistence.query.Query;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.model.DocumentCollection;
import com.kaibla.hamster.persistence.attribute.DateAttribute;
import com.kaibla.hamster.persistence.attribute.PasswordAttribute;
import com.kaibla.hamster.persistence.attribute.StringAttribute;
import com.mongodb.client.MongoDatabase;
import java.util.Locale;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class Users extends DocumentCollection {

    public static Document DEFAULT_USER;
    public final static StringAttribute NAME = new StringAttribute(Users.class, "name");
    public final static PasswordAttribute PASSWORD = new PasswordAttribute(Users.class, "password");
    public final static PasswordAttribute PASSWORD2 = new PasswordAttribute(Users.class, "password2");
    public final static StringAttribute MAIL = new StringAttribute(Users.class, "mail", true);

    public final static DateAttribute CREATED = new DateAttribute(Users.class, "created");
    public final static DateAttribute LAST_LOGIN = new DateAttribute(Users.class, "last_login");

  
    public Users(UIEngine engine, MongoDatabase db,String name) {
        super(engine, db,name);       
        if (DEFAULT_USER == null) {
            //create new default user
            DEFAULT_USER = createNew().set(NAME, "default_user").set(PASSWORD, "").set(MAIL, "default_user@kaibla.com");
        } else {
            DEFAULT_USER.set(PASSWORD, "");
        }
        DEFAULT_USER.addHolder(new ChangedListener() {

            @Override
            public void dataChanged(DataEvent e) {
            }

            @Override
            public boolean isDestroyed() {
                return false;
            }
        });    
    }



    public Document getUserByName(String name) {
        return queryOne(new Query().equals(NAME, name));
    }

    private static final Logger LOG = getLogger(Users.class
            .getName());

}
