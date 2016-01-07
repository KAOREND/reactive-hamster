package com.kaibla.hamster.base;

import com.kaibla.hamster.base.Resumable;
import com.kaibla.hamster.data.Users;
import com.kaibla.hamster.persistence.model.Document;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Locale;


/**
 *
 * @author kai
 */
public class HamsterSession implements Serializable, Resumable {
    private static final long serialVersionUID = 1L;

    private transient HashSet<HamsterPage> activePages = new HashSet<HamsterPage>();
    private Document user;
    private String id;
    private Locale locale;
    private UIEngine engine;

    public HamsterSession(UIEngine engine) {
        id = "" + hashCode();
        user = Users.DEFAULT_USER;
        this.engine = engine;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    
    public String getId() {
        return id;
    }

    public synchronized void setLocale(Locale locale) {
//        boolean needsUpdate=false;
//        if(this.locale != null && locale.equals(this.locale)) {
//            needsUpdate=true;
//        }
        this.locale = locale;
//        if(needsUpdate) {
//            updateAllPages();
//        }
        UIContext.setLocale(locale);
    }
    

    
    public synchronized Locale getLocale() {
        return locale;
    }

    public synchronized void loginUser(final Document user) {
        this.user = user;
        if(user != Users.DEFAULT_USER) {
            setLocale(engine.getUserLocale(user));
        }
        updateUserInPages();
    }

//    public void updateAllPages() {
//        for (HamsterPage page : activePages) {
//            final HamsterPage hpage = page;
//            if (UIContext.getPage() == page) {
//                hpage.setUser(user);
//            } else {
//                engine.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        hpage.updateAll();
//                    }
//                }, page);
//            }
//        }
//    }
       
            
    private void updateUserInPages() {
        for (HamsterPage page : activePages) {
            final HamsterPage hpage = page;
            if (UIContext.getPage() == page) {
                hpage.setUser(user);
            } else {
                engine.execute(new Runnable() {
                    @Override
                    public void run() {
                        hpage.setUser(user);
                    }
                }, page.getListenerContainer());
            }
        }
    }

    public Document getUser() {
        return user;
    }

    public synchronized void logout() {
        this.user = Users.DEFAULT_USER;
        updateUserInPages();
    }

    public synchronized void setUser(Document lastUserLogin) {
        this.user = lastUserLogin;
         if(user != Users.DEFAULT_USER) {
             setLocale(engine.getUserLocale(user));
         }
    }

    public synchronized void addPage(HamsterPage page) {
        if(activePages == null) {
            activePages= new HashSet<HamsterPage>();
        }
        activePages.add(page);
    }

    public synchronized void removePage(HamsterPage page) {
        if(activePages == null) {
            activePages= new HashSet<HamsterPage>();
        }
        activePages.remove(page);
    }

    public HashSet<HamsterPage> getActivePages() {
        if(activePages == null) {
            activePages= new HashSet<HamsterPage>();
        }
        return activePages;
    }

    public synchronized boolean isActive() {
        return !activePages.isEmpty();
    }

    @Override
    public void resume() {       
    }
}
