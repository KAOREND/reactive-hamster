/*
 * HamsterPage.java Created on 18. Februar 2007, 17:31
 */
package com.kaibla.hamster.base;

import com.kaibla.hamster.base.AbstractListenerContainer;
import com.kaibla.hamster.base.Resumable;
import com.kaibla.hamster.base.DataEvent;
import static com.kaibla.hamster.base.UIContext.getEvent;
import com.kaibla.hamster.components.Function;
import com.kaibla.hamster.components.form.FormElement;
import com.kaibla.hamster.data.Users;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.util.CloneMap;
import com.kaibla.hamster.util.Template;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import static java.lang.System.currentTimeMillis;
import java.lang.reflect.Field;
import java.util.ArrayList;
import static java.util.Collections.synchronizedMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.Session;

/**
 * @author Kai Orend
 */
public abstract class HamsterPage extends HamsterComponent {

    private static final long serialVersionUID = 1L;

    protected UIEngine engine;
    protected int linkCount = 0;
    protected ModificationManager modManager;
    private final HashMap staticActions = new HashMap();
    private final HashMap allStaticActions = new HashMap();
    private boolean destroyed = false;
    private HashMap templates = new HashMap();
    private boolean isTemplateCache;
    private String staticPath;
    private HashMap js;
    private HashMap css;
    private String relativePath = "";
    private String useragent = "";
    private String ip = "";

    private transient AsyncContext currentEvent;
    private transient Session webSocketSession;
    protected long lastAliveTest = 0;
    private transient Map<String, HamsterComponent> componentMap = synchronizedMap(new HashMap());
    protected Document user = Users.DEFAULT_USER;
    private HamsterSession session;
    // the form element which currently holds the focus
    private FormElement focusedElement = null;
    private String titlePrefix = "";
    private String title = "";
    private boolean mobile = false;
    private transient LinkedList<Function> cleanUpHandler = new <Function>LinkedList();
    private transient boolean userActive = true;
    private static final int PAGE_ALIVE_TIME = 120000;
    private HashSet<HamsterComponent> onShowEventListeners;
    private transient HashSet<HamsterComponent> firedOnShowEvent;
    private transient HamsterPage self = this;

    private Template template = BODY;

    AbstractListenerContainer<HamsterPage> listenerContainer;

    public HamsterPage(UIEngine engine, HamsterSession session) {
        super();
        this.engine = engine;
        createListenerContainer();
        this.session = session;
        session.addPage(this);
        UIContext.setPage(this);
        setUser(session.getUser());

        engine.registerConnection(this, getEvent());
        page = this;
        addedToParent = true;
        this.engine = engine;
        engine.addComponent(this);
        components = new ArrayList();
        modManager = new ModificationManager(this);
        checkUserAgent(UIContext.getRequest());
        js = new HashMap();
        css = new HashMap();
        markAsAlive();
        visible = true;
        //locale=request.getLocales().nextElement();
    }

    public HamsterPage() {
        super();
        engine = UIEngine.getEngine();
        createListenerContainer();
        components = new ArrayList();
        modManager = new ModificationManager(this);
        markAsAlive();
    }

    private void createListenerContainer() {
        listenerContainer = new AbstractListenerContainer<HamsterPage>(engine, this) {
            private static final long serialVersionUID = 1L;

            @Override
            public void dataChanged(DataEvent e) {
                self.dataChanged(e);
            }

            @Override
            public boolean isDestroyed() {
                return self.isDestroyed();
            }
        };
    }

    @Override
    public AbstractListenerContainer getListenerContainer() {
        return listenerContainer;
    }

    private HashSet<HamsterComponent> getFiredOnShowEvent() {
        if (firedOnShowEvent == null) {
            firedOnShowEvent = new HashSet<>();
        }
        return firedOnShowEvent;
    }

    private HashSet<HamsterComponent> getFiredOnShowEventListeners() {
        if (onShowEventListeners == null) {
            onShowEventListeners = new HashSet<>();
        }
        return onShowEventListeners;
    }

    public void registerOnShowListener(HamsterComponent comp) {
        getFiredOnShowEventListeners().add(comp);
    }

    public void removeOnShowListener(HamsterComponent comp) {
        getFiredOnShowEventListeners().remove(comp);
        getFiredOnShowEvent().remove(comp);
    }

    public void fireOnShow() {
        for (HamsterComponent listener : getFiredOnShowEventListeners()) {
            boolean wasFired = getFiredOnShowEvent().contains(listener);
            if (!wasFired && listener.isVisible()) {
                //component got visibile, fire onShow and remember that it was fired
                getFiredOnShowEvent().add(listener);
                listener.onShow();
            } else if (wasFired && !listener.isVisible()) {
                //component is not visible anymore remove it from fired list
                getFiredOnShowEvent().remove(listener);
                //here would be a good place to fire onHidden event, if somebody would care about that.
            }
        }
    }

    public boolean isMobile() {
        return mobile;
    }

    public void setUserActive(boolean userActive) {
        this.userActive = userActive;
    }

    public boolean isUserActive() {
        return userActive;
    }

    public Lock getLock() {
        return listenerContainer.getLock();
    }

    public String getIp() {
        return ip;
    }

    public String getUseragent() {
        return useragent;
    }

    public void addCleanUpHander(Function function) {
        if (cleanUpHandler == null) {
            cleanUpHandler = new LinkedList<Function>();
        }
        cleanUpHandler.add(function);
    }

    public void setTitlePrefix(String titlePrefix) {
        this.titlePrefix = titlePrefix;
        title = titlePrefix;
    }

    public void setTitle(String title) {
        this.title = titlePrefix + " - " + decode(title);
        exec("document.title='"
                + this.title
                + "'");
    }

    public String getTitle() {
        return title;
    }

    public HamsterSession getSession() {
        return session;
    }

    public void setSession(HamsterSession session) {
        this.session = session;
    }

    public void setWebSocketSession(Session webSocketSession) {
        this.webSocketSession = webSocketSession;
    }

    public Session getWebSocketSession() {
        return webSocketSession;
    }

    public String getResources() {
        return "";
    }

    public Map<String, HamsterComponent> getComponentMap() {
        if (componentMap == null) {
            componentMap = synchronizedMap(new HashMap());
        }
        return componentMap;
    }

    public void setFocusedElement(FormElement focusedElement) {
        this.focusedElement = focusedElement;
    }

    public FormElement getFocusedElement() {
        return focusedElement;
    }

    /**
     * @return the user
     */
    public Document getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(Document user) {
        Document oldUser = this.user;
        this.user = user;
        if (oldUser != user && oldUser != null && oldUser != Users.DEFAULT_USER) {

            getEngine().removeUserPage(oldUser.getId(), this);

            getEngine().addUserPage(user.getId(), this);
            onLogout(oldUser, user);
        }
        if (oldUser != user && user != Users.DEFAULT_USER) {
            updateAll();
            onLogin(user);
        }
        getEngine().addUserPage(user.getId(), this);
    }

    public void onLogin(Document user) {

    }

    public void onLogout(Document oldUser, Document newUser) {

    }

    public void logout() {
        session.logout();
    }

    public void addJavaScript(String url) {
        if (js.get(url) == null) {
            js.put(url, url);
            exec("loadJS('" + url + "')");

        }
    }

    public void addCSS(String url) {
        if (css.get(url) == null) {
            css.put(url, url);
            exec("loadCSS('" + url + "')");
        }
    }

    public void checkUserAgent(HttpServletRequest request) {
        if (request != null) {
            useragent = request.getHeader("User-Agent");
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            if (ip == null) {
                ip = request.getRemoteAddr();
            }
            //LOG.info("useragent: " + useragent);
            if (useragent != null && useragent.toLowerCase().indexOf("gecko") != -1) {
                isTemplateCache = true;
            } else {
                isTemplateCache = false;
            }
            if (useragent != null && useragent.toLowerCase().contains("mobi")) {
                mobile = true;
            }
        }
        isTemplateCache = false;
    }

    public String getUserAgent() {
        return useragent;
    }

    public boolean isTemplateCache() {
        return isTemplateCache;
    }

    public boolean isTemplateInCache(Template t) {
        return templates.get(t) == t;

    }

    public void markTemplateAsCached(Template t) {
        templates.put(t, t);
    }

    public boolean makingXMLRequest() {
        return modManager.working;
    }

    public void registerStaticAction(Action a, HamsterComponent comp) {
        // LOG.info("registerAction : " + a.getClass().getName()
        // + " by: " + Thread.currentThread().getStackTrace()[1]);
        Action old = (Action) staticActions.get(a.getStaticName());
        if (old != null) {
            old.firePageLeftEvent();
        }
        staticActions.put(a.getStaticName(), a);
        allStaticActions.put(a.getStaticName(), a);
        //comp.setLastStaticAction(a);
    }

    public void addUnactiveStaticAction(Action a) {
        if (allStaticActions.get(a.getStaticName()) == null) {
            allStaticActions.put(a.getStaticName(), a);
        }
    }

    public void unregisterStaticAction(Action a, HamsterComponent comp) {
        // LOG.info("unregisterAction : " + a.getClass().getName()
        // + " by: " + Thread.currentThread().getStackTrace()[1]);
        if (comp.getLastStaticAction() == null) {
            LOG.log(Level.SEVERE, "lastStaticAction lost: {0}  a:{1}", new Object[]{comp.getClass().
                getName(), a.getClass().getName()});
        }

        Action old = (Action) staticActions.get(a.getStaticName());
        if (old != null) {
            old.firePageLeftEvent();
        }
        staticActions.remove(a.getStaticName());
    }

    public int getNewLinkId() {
        return linkCount++;
    }

    public UIEngine getEngine() {
        return engine;
    }

    public String getRefreshLink() {
        return getPage().getXLink(new LinkedList());
    }

    public final ModificationManager getModificationManager() {
        return modManager;
    }

    @Override
    public synchronized String getHTMLCode() {
        String result = super.getHTMLCode();
        fireOnShow();
        return result;
    }

    public synchronized String getHTMLCodeForPageCreation() {
//        super.getHTMLCode();
        String initialHTML = getInitialHTMLCode();
        page.fireOnShow();
        htmlCode = null; //cleanup buffer
        getModificationManager().reset();
        return initialHTML;
    }

    public String getInitialHTMLCode() {
        setUserActive(true);
        LinkedList slots = new LinkedList();
        slots.add(getTitle());
        slots.add(getPage().getHLink(new LinkedList())); // Link fuer die
        // HistoryLoop
        slots.add(getRefreshLink()); // Link fuer die
        slots.add("?z?" + getId() + "?" + getId() + "?");

        slots.add(getContextRoot()); //set the context root
        slots.add(getTranslatedString("notificationPermissionQuestions"));
        StringBuilder cBuilder = new StringBuilder();
        for (HamsterComponent comp : components) {
            cBuilder.append(comp.getHTMLCode());
        }
        //slots.addAll(components);
        fireOnShow();
        slots.add(modManager.getAllScripts());
        slots.add(cBuilder.toString());

        modManager.clearScripts();
        System.out.println("getInitialHTMLCode scripts2 " + modManager.getAllScripts());

        htmlCode = template.
                mergeStrings(slots, getPage(), "RELATIVE_PATH", getRelativePath());
        return htmlCode;
    }

    public String getContextRoot() {
        String contextRoot = "/";
        if (UIContext.getRequest() != null) {
            contextRoot = UIContext.getRequest().getContextPath();
            if (contextRoot == null || contextRoot.isEmpty()) {
                contextRoot = "/";
            } else {
                contextRoot += "/";
            }
        }
        return contextRoot;
    }

    @Override
    public void generateHTMLCode() {
        htmlCode = "";
        for (HamsterComponent comp : components) {
            htmlCode += comp.toString();
        }
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public void markAsAlive() {
        lastAliveTest = currentTimeMillis();
    }

    public boolean checkAlive() {
//        if (webSocketSession != null && webSocketSession.isOpen()) {
//            return true;
//        }
        long timeDiff = currentTimeMillis() - lastAliveTest;
        return timeDiff <= PAGE_ALIVE_TIME;
    }

//    public void setUserInfo(String newUserid, String newUserhash) {
//        if (!userid.equals(newUserid) || !userhash.equalsIgnoreCase(newUserhash)) {
//            getEngine().removeUserPage(userid, this);
//            userid = newUserid;
//            userhash = newUserhash;
//            getEngine().addUserPage(newUserid, this);
//            updateAll();
//        }
//    }
//
//    public String getUserId() {
//        return userid;
//    }
//
//    public String getUserHash() {
//        return userhash;
//    }
    @Override
    public void copyFrom(HamsterComponent orig, CloneMap map) {
        super.copyFrom(orig, map);
        modManager = new ModificationManager(this);
        templates = new HashMap();
    }

    public String getStaticPath(Action action) {
        return getStaticPath(action, false);
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void resetRelativePath() {
        relativePath = "";
    }

    public String getEnginePath() {
        return getRelativePath() + getServletName();
    }

    @Override
    public void reconstruct(Reconstructor rec) {
        Iterator iter = rec.list().iterator();
        if (rec.createRelativePath) {
            while (iter.hasNext()) {
                iter.next();
                relativePath += "../";
                if (iter.hasNext()) {
                    relativePath += "../";
                }
            }
        }
        super.reconstruct(rec);
    }

    public String getServletName() {
        return "";
    }

    private String getStaticPath(Action action, boolean leaveEvents) {
        String path = "";
        if (action != null) {
            path = action.getStaticString();
        }
        LinkedList deps = new LinkedList();

        Action p = action;
        while (p != null) {
            if (p.getStaticParent() != null) {
                Action a = (Action) allStaticActions.get(p.getStaticParent());
                if (a == null) {
                    LOG.log(Level.INFO, "action parent not found: {0} {1}", new Object[]{p.getClass().
                        getName(), p.getStaticParent()});
                }
                p = a;
                if ((p != null) && !deps.contains(p)) {
                    deps.add(p);
                }
            } else {
                p = null;
            }
        }
        HashMap pathMap = new HashMap();
        Iterator iter = deps.iterator();
        while (iter.hasNext()) {
            Action a = (Action) iter.next();
            if ((action == null) || (!a.getStaticName().equalsIgnoreCase(action.
                    getStaticName())) || ((a.getClass() != action.getClass()) && (a.
                    getClass().getDeclaringClass() != action.getClass().
                    getDeclaringClass()))) {
                String prefix = "";
//                if (a.getStaticPrefix() != null) {
//                    prefix = a.getStaticPrefix() + "?";
//                }
                pathMap.put(a.getStaticName(), a);
                path = a.getStaticString() + "/" + path;
            }
        }
        if (leaveEvents) {
            Iterator iter2 = staticActions.values().iterator();
            while (iter2.hasNext()) {
                Action a = (Action) iter2.next();
                Action newA = (Action) pathMap.get(a.getStaticName());
                if (newA == null) {
                    a.firePageLeftEvent();
                } else if (!newA.getStaticParameter().equals(a.
                        getStaticParameter())) {
                    a.firePageLeftEvent();
                }
            }
        }
        return path;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public void destroy() {
        Document localUser = getUser();
        //setUser(Users.DEFAULT_USER);
        if (user != null) {
            getEngine().removeUserPage(user.getId(), page);
        }
        onPageClose();
        if (session != null) {
            session.removePage(this);
            if (!session.isActive()) {

                engine.removeSession(session);
                onSessionClose();

            }
        }
        if (destroyed) {
            return;
        }
        destroyed = true;

        Iterator iter = allStaticActions.values().iterator();
        while (iter.hasNext()) {
            Action a = (Action) iter.next();
            a.firePageLeftEvent();
        }
        for (HamsterComponent comp : components) {
            comp.destroy();
        }
        if (webSocketSession != null) {
            try {
                webSocketSession.close();
            } catch (IOException ex) {
                Logger.getLogger(HamsterPage.class.getName()).log(Level.SEVERE, "closing websocket failed", ex);
            }
        }
        if (cleanUpHandler != null) {
            for (Function handler : cleanUpHandler) {
                handler.invoke();
            }
        }
        super.destroy();
    }

    public void onPageClose() {
    }

    ;
    
    public void onSessionClose() {
    }

    ;
    
    
    
    

    public String getStaticPath() {
        return staticPath;
    }

    public void setHashURL(Action action) {
        if (action != null) {
            staticPath = getPage().getStaticPath(action);
            updateHashURL();
        }
    }

    public void updateHashURL() {
        if (staticPath != null) {
            exec("hamster.main.updateHashURL('" + staticPath + "');");
        }
    }

    public void setStaticPath(Action action) {
        this.staticPath = getStaticPath(action, true);
    }

    public void setStaticPath(String staticPath) {
        this.staticPath = staticPath;
    }

    /**
     * @return the currentEvent
     */
    public AsyncContext getCurrentEvent() {
        return currentEvent;
    }

    /**
     * @return the currentConnection
     */
    public ServletResponse getCurrentConnection() {
        if (currentEvent == null) {
            return null;
        }
        return currentEvent.getResponse();
    }

    /**
     * @param currentConnection the currentConnection to set
     */
    public void setCurrentConnection(AsyncContext currentEvent) {

        this.currentEvent = currentEvent;

    }

    public void persist(OutputStream s) {
        UIContext.setPage(this);
//        engine.debugMemoryAndThreadLocals(page);
        for (HamsterComponent comp : new ArrayList<HamsterComponent>(componentMap.values())) {
            comp.prePersist();
        }
        modManager.reset();
//        engine.debugMemoryAndThreadLocals(page);
        try {
            ObjectOutputStream os = new HamsterObjectOutputStreamImpl(new DeflaterOutputStream(s));
            os.writeObject(this);
            os.close();
        } catch (IOException ex) {
            getLogger(HamsterPage.class.getName()).
                    log(Level.SEVERE, null, ex);
        } finally {
            try {
                s.close();
            } catch (IOException ex) {
                getLogger(HamsterPage.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        }
    }

    public static HamsterPage resume(InputStream in, UIEngine engine) {
        HamsterPage page = null;
        HamsterObjectInputStreamImpl io = null;
        try {
            io = new HamsterObjectInputStreamImpl(new InflaterInputStream(in), engine);

            page = (HamsterPage) io.readObject();
            for (HamsterPage p : io.getPages()) {
                if (p != page) {
                    searchPathToObject(new HashSet<>(), new LinkedList(), p, page);
                    searchPathToObject(new HashSet<>(), new LinkedList(), page, p);
                }
            }

            page.getLock().lock();
            try {
                UIContext.setPage(page);
                engine.addComponent(page);
                page.markAsAlive();
                HamsterSession existingSession = UIContext.getSession();
                if (existingSession == null) {
                    existingSession = engine.getSession(page.session.getId());
                }
                engine.addUserPage(page.session.getUser().getId(), page);
                io.finish();
                page.getListenerContainer().flushPending();
                if (existingSession != null) {
                    HamsterSession oldSession = page.session;
                    page.session = existingSession;
                    existingSession.addPage(page);
                    existingSession.loginUser(oldSession.getUser());
                } else {
                    engine.putSession(page.session);
                    page.session.addPage(page);
                    page.session.loginUser(page.session.getUser());
                    UIContext.setSession(page.session);
                }

//                if (existingSession != page.session) {
//                    if (existingSession != null) {
//                        LOG.info("logging in user from resumed session into exisiting one " + page.session.getUser());
//
//                        existingSession.loginUser(page.session.getUser());
//                        existingSession.addPage(page);
//                        page.session=existingSession;
//                    } else {                       
//                        
//                        LOG.info("setting resumed session as current session  " + page.session.getUser() + "  session " + page.session.getId());
//                        
//                        
//                        engine.putSession(page.session);
//                        page.session.addPage(page);
//                        UIContext.setSession(page.session);
////                    page.exec("document.cookie = 'sid=\""
////                            +  page.session.getId()
////                            + "\";path=\"/\";'");
//
//                    }
//                }
                for (HamsterComponent comp : io.getComponents()) {
                    page.getComponentMap().put(comp.getId(), comp);
                }
                for (HamsterComponent comp : io.getComponents()) {
                    comp.afterResume();
                }
                engine.checkSessionAndPage(page);
            } finally {
                page.getLock().unlock();
            }
        } catch (IOException ex) {
            getLogger(HamsterPage.class.getName()).
                    log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            getLogger(HamsterPage.class.getName()).
                    log(Level.SEVERE, null, ex);
        } finally {
            if (io != null) {
                try {
                    io.close();
                } catch (IOException ex) {
                    getLogger(HamsterPage.class.getName()).
                            log(Level.SEVERE, null, ex);
                }
            }
        }
        return page;
    }

    private class HamsterObjectOutputStreamImpl extends ObjectOutputStream {

        public HamsterObjectOutputStreamImpl(OutputStream out) throws IOException {
            super(out);
            enableReplaceObject(true);
        }

        @Override
        protected Object replaceObject(Object obj) throws IOException {
            if (obj instanceof HamsterComponent) {
                HamsterComponent cmp = (HamsterComponent) obj;
                if (cmp.getPage() != UIContext.getPage()) {
                    searchPathToObject(new HashSet<>(), new LinkedList(), UIContext.getPage(), cmp.getPage());
                    throw new IllegalStateException("Cannot persist HamsterComponent of another page");
                }
//                if (cmp.isDestroyed()) {
//                    System.out.println("Cannot persist destroyed component" + cmp.getClass());
//                    System.out.println("________________paths from cmp.getPage() to cmp_____________:");
//                    engine.debugMemoryAndThreadLocals(cmp.getPage());
//                     engine.debugMemoryAndThreadLocals(cmp);
//                    //searchPathToObject(new HashSet<>(), new LinkedList(), cmp.getPage(), cmp);
////                    return null;
//                } //else if (cmp.getParent() != null && !cmp.getParent().components.contains(cmp)) {
//
//                    System.out.println("Cannot persist HamsterComponent which is not child of its parent " + cmp.getClass());
//                    System.out.println("________________paths from cmp to cmp.getPage()_____________:");
//                    searchPathToObject(new HashSet<>(), new LinkedList(), cmp, cmp.getPage());
//                    System.out.println("________________paths from cmp.getPage() to cmp_____________:");
//                    searchPathToObject(new HashSet<>(), new LinkedList(), cmp.getPage(), cmp);
//                    //throw new IllegalStateException("Cannot persist HamsterComponent which is not child of its parent"); 
//                }
            }
//            if(obj instanceof Resumeable) {
//                return ((Resumeable)obj).prepareStore(engine);
//            }            
            return super.replaceObject(obj);
        }
    }
//

    private static void searchPathToObject(HashSet<Object> visited, LinkedList path, Object startObject, Object endObject) {
        if (visited.contains(startObject) || startObject == null) {
            return;
        }
        visited.add(startObject);
        path.add(startObject);
        Class c = startObject.getClass();
        while (c != null) {
            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                path.add(field);
                try {
                    Object value = field.get(startObject);
                    if (value == endObject) {
                        System.out.println("________________found path to object_____________:");
                        for (Object o : path) {
                            if (o instanceof Field) {
                                System.out.println(((Field) o).getName());
                            } else {
                                System.out.println(o.getClass());
                            }
                        }
                    }
                    //	LOG.info("dm: " + c.getName() + " " + fields[i].getName() + " = " + value);
                    searchPathToObject(visited, path, value, endObject);
                } catch (IllegalArgumentException ex) {
                    LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
                } catch (IllegalAccessException ex) {
                    LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
                } finally {
                    path.removeLast();
                }
            }
            //LOG.info("dm: superclass: "+c.getSuperclass()+" von "+c);
            c = c.getSuperclass();
        }
        path.removeLast();
    }

    private static class HamsterObjectInputStreamImpl extends ObjectInputStream {

        UIEngine engine;
        HashSet<Resumable> resumables = new HashSet<Resumable>();
        HashSet<HamsterPage> pages = new HashSet<>();
        HashSet<HamsterComponent> components = new HashSet<>();
        long startTime = 0;

        public HamsterObjectInputStreamImpl(InputStream in, UIEngine engine) throws IOException {
            super(in);
            startTime = System.currentTimeMillis();
            enableResolveObject(true);
            this.engine = engine;
        }

        public HashSet<HamsterPage> getPages() {
            return pages;
        }

        public HashSet<HamsterComponent> getComponents() {
            return components;
        }

        @Override
        protected Object resolveObject(Object obj) throws IOException {
            long spentTime = System.currentTimeMillis() - startTime;
//            if (spentTime > 5000) {
//                throw new RuntimeException("resuming a page should not take longer than 5 seconds, aborting");
//                //System.out.println(obj.getClass());
//            }
            obj = super.resolveObject(obj);
            if (obj instanceof HamsterComponent) {
                HamsterComponent comp = (HamsterComponent) obj;
                components.add(comp);
                HamsterPage page = comp.page;
                if (comp instanceof HamsterPage) {
                    page = (HamsterPage) comp;
                }
                if (page != null) {
                    //ensure that the page is registered as early as possible, so that it can be cleaned up should the resume fail
                    engine.addPage(page);
                    page.self=page;
                    page.markAsAlive();
                    if (!pages.contains(page)) {
                        pages.add(page);
                    }
//                    if(pages.size() > 0) {
//                      System.out.println("found more than one page: "+pages.size());  
//                    }
//                    if (page1 == null) {
//                        page1 = page;
//                    } else if (page1 != page && page != page2) {
//                        page2=page;
//                        searchPathToPage(new HashSet<>(),new LinkedList(),page1,page);
//                        searchPathToPage(new HashSet<>(),new LinkedList(),page,page1);
//                        searchPathToPage(new HashSet<>(),new LinkedList(),obj,page1);
//                        throw new RuntimeException("there cannot be two pages in one persisted session!");
//                    }
//                    if(page1 != null && page2 != null) {
//                        searchPathToPage(new HashSet<>(),new LinkedList(),page1,page);
//                        searchPathToPage(new HashSet<>(),new LinkedList(),page,page1);
//                        searchPathToPage(new HashSet<>(),new LinkedList(),obj,page1);
//                    }
                }
            }
            if (obj == null) {
                return null;
            }
            Class c = obj.getClass();
            if (c.getName().startsWith("javax")) {
                throw new RuntimeException("there should be no javax objects in the stored session");
            }
            if (obj instanceof Resumable) {
                resumables.add((Resumable) obj);
            }
            return obj;
        }

        public void finish() {
            for (Resumable r : resumables) {
                r.resume();
            }
        }

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            ObjectStreamClass resultClassDescriptor = super.readClassDescriptor(); // initially streams descriptor
            Class localClass; // the class in the local JVM that this descriptor represents.
            try {
                localClass = Class.forName(resultClassDescriptor.getName());
            } catch (ClassNotFoundException e) {
                getLogger(HamsterPage.class.getName()).
                        log(Level.SEVERE, null, e);
                return resultClassDescriptor;
            }
            ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookup(localClass);
            if (localClassDescriptor != null) { // only if class implements serializable
                final long localSUID = localClassDescriptor.getSerialVersionUID();
                final long streamSUID = resultClassDescriptor.getSerialVersionUID();
                if (streamSUID != localSUID) { // check for serialVersionUID mismatch.
                    final StringBuffer s = new StringBuffer("Overriding serialized class version mismatch: ");
                    s.append("local serialVersionUID = ").append(localSUID);
                    s.append(" stream serialVersionUID = ").append(streamSUID);
                    Exception e = new InvalidClassException(s.toString());
                    getLogger(HamsterPage.class.getName()).
                            log(Level.SEVERE, null, e);
                    resultClassDescriptor = localClassDescriptor; // Use local class descriptor for deserialization
                }
            }
            return resultClassDescriptor;
        }
    }

    public void onPageUpdate() {

    }

    private static transient Template BODY = new Template(HamsterPage.class.
            getResource("page.html"));

    private static final Logger LOG = getLogger(HamsterPage.class.getName());
}
