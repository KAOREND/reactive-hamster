package com.kaibla.hamster.base;

import static com.kaibla.hamster.base.UIContext.setEvent;
import static com.kaibla.hamster.base.UIContext.setPage;
import static com.kaibla.hamster.base.UIContext.setRequest;
import static com.kaibla.hamster.base.UIContext.setResponse;
import static com.kaibla.hamster.base.UIContext.setUser;
import static com.kaibla.hamster.base.HamsterLoader.reconstructAction;
import static com.kaibla.hamster.base.HamsterPage.resume;
import com.kaibla.hamster.data.PersistedPages;
import com.kaibla.hamster.data.Users;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.servlet.CometProcessor;
import com.kaibla.hamster.servlet.WebSocket;
import com.kaibla.hamster.monitoring.AutomaticMonitoring;
import com.kaibla.hamster.util.CloneMap;
import com.kaibla.hamster.monitoring.DeadLockMonitor;
import com.mongodb.Block;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.gc;
import static java.lang.Thread.yield;
import java.lang.reflect.Field;
import static java.net.URLDecoder.decode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import static java.lang.Integer.parseInt;
import static java.net.URLDecoder.decode;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public abstract class UIEngine extends HamsterEngine {

    //    private boolean mapLocked = false;
    private final Map<String, HamsterPage> pages = new ConcurrentHashMap();
    private final Map<String, Set<HamsterPage>> userPages = new ConcurrentHashMap();
    private final boolean debug = false;

    private static final LinkedList errors = new LinkedList();
    private static final Map staticActionComps = new ConcurrentHashMap();

    private String name;
    private final ConcurrentHashMap<AsyncContext, HamsterPage> connections = new ConcurrentHashMap<AsyncContext, HamsterPage>(100);
    private final ConcurrentHashMap<String, HamsterSession> sessions = new ConcurrentHashMap<String, HamsterSession>(100);

    private static UIEngine engine;
    AtomicLong globalCounter = new AtomicLong(0);
    AtomicLong globalProcessingTime = new AtomicLong(0);
    AtomicLong shortCounter = new AtomicLong(0);
    AtomicLong shortProcessingTime = new AtomicLong(0);
    HamsterLoader hamsterLoader = null;
    /**
     * GridFS used for storing persisted sessions
     */
    private GridFSBucket gridFS;

    private PersistedPages persistedPages;

    public static final boolean BENCHMARK_MODE = false;
//	private String serverAdress=null;

    /**
     * Creates a new instance of ComponentManager
     */
    public UIEngine() {
        //serverAdress=servlet.getServletContext().;
        //LOG.info("Server: "+serverAdress);
        // name = servlet.getServletContext().getServletContextName();
        //LOG.info("Servername: "+name); 
        if (engine != null) {
            throw new RuntimeException("there cannot be two engines at once");
        }
        engine = this;
        ImageIO.scanForPlugins();
    }

    public void initDB(MongoDatabase db, GridFSBucket gridFS) {
        this.gridFS = gridFS;
        persistedPages = new PersistedPages(engine, db, gridFS);
        super.initDB(db);
    }

    public static UIEngine getEngine() {
        return engine;
    }

    @Override
    protected void executeSynchronouslyInternal(final Runnable runnable, final AbstractListenerContainer container) {
        final AbstractListenerContainer<HamsterPage> pageContainer = container;
        try {
            //start transaction
            clear();
            if (container != null && container.getOwner() != null) {
                UIContext.setUser(pageContainer.getOwner().getUser());
                UIContext.setLocale(getUserLocale(pageContainer.getOwner().getUser()));
                UIContext.setPage(pageContainer.getOwner());
                getTransactionManager().runInTransaction(new Runnable() {
                    @Override
                    public void run() {
                        AutomaticMonitoring.run(runnable, 5000, "page: " + (pageContainer.getOwner() == null ? null : pageContainer.getOwner().getId()));
                    }
                },5);
                updateOtherPages();
            } else {
                super.executeSynchronouslyInternal(runnable, container);
            }
            clear();
        } finally {
            clear();
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

//	public String getServerAdress() {
//		return serverAdress;
//	}
    public synchronized void doLazyInit() {
        if (!isInitilized()) {
            init();
        }
    }

    public synchronized void init() {
        super.init();
//      Thread.currentThread().dumpStack();     

//        	try { 
//	    new Logger(false);
//	} catch (Exception ex) {
//	   LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
//	}
        LOG.info("loading HamsterLoader");
        hamsterLoader = new HamsterLoader(this);
        Thread t = new Thread(this); //cleaner thread
        t.setDaemon(true);
        t.setName("hamster cleanup thread");
        t.start();
        t.setPriority(Thread.MIN_PRIORITY);
        this.addThread(t);
        LOG.info("init finished");
        startDeadLockMonitor();
        this.addThread(AutomaticMonitoring.startMonitoring());
    }

    public HamsterLoader getHamsterLoader() {
        return hamsterLoader;
    }

    public void reportRequestProcssing(long startTime, long endTime) {
        long duration = endTime - startTime;
        globalCounter.incrementAndGet();
        globalProcessingTime.addAndGet(duration);
        if (shortCounter.get() > 10) {
            shortCounter.set(0);
            shortProcessingTime.set(0);
        }
        shortCounter.incrementAndGet();
        shortProcessingTime.addAndGet(duration);
    }

    private double getAverageReponseTime() {
        long counter = globalCounter.get();
        long time = globalProcessingTime.get();
        if (counter != 0 && time != 0) {
            return time / counter;
        } else {
            return 0;
        }
    }

    private double getAverageReponseShortTime() {
        long counter = shortCounter.get();
        long time = shortProcessingTime.get();
        if (counter != 0 && time != 0) {
            return time / counter;
        } else {
            return 0;
        }
    }

    @Override
    public void updateListenerContainer(AbstractListenerContainer listenerContainer) {
        updateSinglePageSynchronously((HamsterPage) listenerContainer.getOwner());
    }

    public void updateSinglePageSynchronously(final HamsterPage page) {
        try {
            if (page.getWebSocketSession() != null) {
                UIContext.setUser(page.user);
                UIContext.setLocale(getUserLocale(page.user));
                UIContext.setPage(page);

                if (!page.getModificationManager().isEmpty()) {
                    String response = page.getModificationManager().
                            getModificiationXML();
                    if (response != null) {
                        final int confirmCount = page.getModificationManager().getConfirmationCounter();
                        final HamsterPage myPage = page;
                        //session.getBasicRemote().sendText(response);
                        Session session = page.getWebSocketSession();
                        if (session != null && session.isOpen()) {
                            WebSocket.sendMessage(session, response, new SendHandler() {
                                @Override
                                public void onResult(SendResult sr) {
                                    if (sr.isOK()) {
                                        try {
                                            if (!myPage.getLock().tryLock(10, TimeUnit.SECONDS)) {
                                                Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, "Could not aquire page lock after 10 seconds");
                                                return;
                                            }

                                        } catch (InterruptedException ex) {
                                            Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, null, ex);
                                            return;
                                        }
                                        try {
                                            myPage.getModificationManager().confirmLastModificationXML(confirmCount);
                                        } finally {
                                            myPage.getLock().unlock();
                                        }
                                    } else {
                                        getLogger(UIEngine.class.
                                                getName()).
                                                log(Level.SEVERE, null, sr.getException());
                                    }
                                }
                            });
                        }
                    }
                }
                return;
            }
        } catch (Exception ex) {
            getLogger(UIEngine.class.
                    getName()).
                    log(Level.SEVERE, null, ex);
        } finally {
            UIContext.clear();
        }

        final AsyncContext context = page.getCurrentEvent();
        if (context != null) {
            final ServletResponse response = context.getResponse();
            if (response == null) {
                return;
            }
            final String pageResult = page.
                    getModificationManager().
                    getModificiationXML();
            execute(new Runnable() {

                @Override
                public void run() {
                    synchronized (response) {
                        if (page.getCurrentConnection() != null) {
                            try {
                                if (response != null && !response.
                                        isCommitted()) {
                                    UIContext.setUser(page.user);
                                    UIContext.setLocale(getUserLocale(page.user));
                                    UIContext.setPage(page);
                                    PrintWriter writer = page.
                                            getCurrentConnection().
                                            getWriter();
                                    writer.
                                            println(pageResult);

                                    context.getResponse().flushBuffer();
                                    context.complete();
                                }
                            } catch (IOException ex) {
                                getLogger(UIEngine.class.
                                        getName()).
                                        log(Level.SEVERE, null, ex);
                            } finally {
                                UIContext.clear();
                            }
                        }
                    }
                }
            }, null);

        }

    }

    public void removeConnection(final AsyncContext ctx) {

        final HamsterPage page = connections.remove(ctx);
        if (page != null) {
            try {
                if (!page.getLock().tryLock(10, TimeUnit.SECONDS)) {
                    Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, "could not aquire page lock in time");
                    return;
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            try {
                page.setCurrentConnection(null);
            } finally {
                page.getLock().unlock();
            }

        }
    }

    public void registerConnection(final HamsterPage page, final AsyncContext asyncContext) {
//        LOG.info("registering Connection for page "+page.getId());

        try {
            if (!page.getLock().tryLock(10, TimeUnit.SECONDS)) {
                Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, "could not aquire page lock in time");
                return;
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        try {
            AsyncContext oldEvent = page.getCurrentEvent();
            if (oldEvent != null && oldEvent != asyncContext && oldEvent.getResponse() != null && !oldEvent.
                    getResponse().isCommitted()) {
                try {
                    oldEvent.getResponse().flushBuffer();
                    oldEvent.complete();
                } catch (IOException ex) {
                    getLogger(UIEngine.class.getName()).
                            log(Level.SEVERE, null, ex);
                }
            }
            page.setCurrentConnection(asyncContext);
        } finally {
            page.getLock().unlock();
        }

        if (asyncContext != null) {
            asyncContext.addListener(new AsyncListener() {

                @Override
                public void onComplete(AsyncEvent event) throws IOException {
                    removeConnection(asyncContext);
                    page.setCurrentConnection(null);
                }

                @Override
                public void onTimeout(AsyncEvent event) throws IOException {
                    removeConnection(asyncContext);
                    page.setCurrentConnection(null);
                }

                @Override
                public void onError(AsyncEvent event) throws IOException {
                    removeConnection(asyncContext);
                    page.setCurrentConnection(null);
                }

                @Override
                public void onStartAsync(AsyncEvent event) throws IOException {
                }
            });
            connections.put(asyncContext, page);
        }
    }

    public HamsterPage getPageOfEvent(AsyncContext event) {
        return connections.get(event);
    }

    public int getCurrentSessionCount() {
        return pages.size();
    }

    public void destroy() {

        addError("Server has been shut down");
        LOG.info("stopping all HamsterThreads");
        super.destroy();
        engine = null;
        LOG.info("persisting all open pages");
        for (HamsterPage page : pages.values()) {
            LOG.info("persisting page " + page.getId());
            try {
                persistPage(page);
            } catch (Exception ex) {
                getLogger(UIEngine.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        }

        LOG.info("Engine stopped");
    }

    public boolean hasUserOpenPages(String user) {
        Set pageSet = userPages.get(user);
        return pageSet != null && !pageSet.isEmpty();
    }

    public void addUserPage(String user, HamsterPage page) {
        Set pageSet = userPages.get(user);
        if (pageSet == null) {
            pageSet = Collections.synchronizedSet(new HashSet());
        }
        pageSet.add(page);
        userPages.put(user, pageSet);

    }

    public void removeUserPage(String user, HamsterPage page) {
        Set pages = userPages.get(user);
        if (pages != null) {
            pages.remove(page);
            if (pages.isEmpty()) {
                userPages.remove(user);
            }
        }
    }

    public void startDeadLockMonitor() {
        new DeadLockMonitor(this);
    }

    @Override
    public boolean periodicCleanup() {
        boolean cleanedUpSomething = false;
        Iterator pagesIterator = pages.values().iterator();
        while (pagesIterator.hasNext()) {
            HamsterPage page = (HamsterPage) pagesIterator.next();
            synchronized (this) {
                if (page.getLock().tryLock()) {
                    try {

                        setPage(page);
                        if (!page.checkAlive()) {
                            cleanedUpSomething = true;
                            persistPage(page);
                            pagesIterator.remove();
                            page.destroy();
                            updateOtherPages();
                            removeUserPage(page.getUser().getId(), page);
                        } else {

                            checkSessionAndPage(page);
                        }
                    } catch (Exception ex) {
                        LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
                    } finally {
                        page.getLock().unlock();
                    }
                }
            }
            clear();
            yield();
        }
        Iterator<HamsterSession> sessionIterator = sessions.values().iterator();
        while (sessionIterator.hasNext()) {
            HamsterSession session = sessionIterator.next();
            if (!session.isActive()) {
                sessionIterator.remove();
                cleanedUpSomething = true;
            }
            yield();
        }
        LOG.log(Level.INFO, "active pages: {0} active sessions {1} active models {2}  avg response time {3}ms  avg response short time {4}ms", new Object[]{pages.
            size(), sessions.size(), countModels(), getAverageReponseTime(), getAverageReponseShortTime()});

        return cleanedUpSomething;
    }

    IdentityHashMap<Object, Object> checkedMap = new IdentityHashMap();
    LinkedList path = new LinkedList();

    public void debugMemoryAndThreadLocals(Object obj) {

        LOG.info("UIEngine:start debugMemory");
        synchronized (checkedMap) {
            checkedMap.clear();
            path.clear();
            try {
                debugMemory(obj);
                Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
                Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
                for (Thread thread : threadArray) {
                    debugMemory(thread);
                }

            } finally {
                checkedMap.clear();
                path.clear();
            }
        }
        LOG.info("UIEngine:finished debugMemory");
    }

    private class DebugPathEntry {

        Object obj;
        Field f;
        DebugPathEntry parent = null;

        public DebugPathEntry(Object obj, Field f, DebugPathEntry parent) {
            this.obj = obj;
            this.f = f;
            this.parent = parent;
        }

        @Override
        public String toString() {
            DebugPathEntry next = this;
            String result = "";
            while (next != null) {
                String e = "";
                if (next.f != null) {
                    e += "." + next.f.getName();
                }
                e += "[" + next.obj.getClass().getName() + "]";

                result = e + result;
                next = next.parent;
            }

            return result;
        }

    }

    private void debugMemory(Object obj) {
        ArrayDeque<DebugPathEntry> queue = new ArrayDeque<DebugPathEntry>();
        queue.add(new DebugPathEntry(obj, null, null));

        while (!queue.isEmpty()) {
            DebugPathEntry nextEntry = queue.removeLast();
            obj = nextEntry.obj;
//        LOG.info("UIEngine:debugMemory:" + path);
            if (obj != path && obj != checkedMap && obj != null) {
//             LOG.info("UIEngine:debugMemory:" + obj);             
                if (obj instanceof HamsterComponent) {
                    HamsterComponent comp = (HamsterComponent) obj;
                    if (comp.getPage().isDestroyed()) {
                        LOG.warning("UIEngine:debugMemory: found  component with destroyed page: " + nextEntry);
                    }
                    if (comp.isDestroyed()) {
                        LOG.warning("UIEngine:debugMemory: found destroyed component: " + nextEntry);
                    }
                    if (comp.getParent() != null && !comp.getParent().components.contains(comp)) {
                        LOG.warning("UIEngine:debugMemory: found component which is no child of its parent: " + nextEntry);
                    }
                }
                if (obj instanceof HamsterPage) {
                    HamsterPage page = (HamsterPage) obj;
                    if (!page.isDestroyed()) {
                        HamsterSession session = page.getSession();
                        if (!session.isActive()) {
                            LOG.warning("UIEngine:debugMemory: found inconsistent session page has session, but page is not active : " + nextEntry);
                        }
                        if (!sessions.contains(session.getId())) {
                            LOG.warning("UIEngine:debugMemory: found inconsistent session page has session, but session is not in session map : " + nextEntry);
                        }

                        if (!session.getActivePages().contains(page)) {
                            LOG.warning("UIEngine:debugMemory: found inconsistent session page has session, but session does not contain page : " + nextEntry);
                        }
                        //test if session of page is reachable
                    }
                    if (pages.get(page.getId()) != page) {
                        LOG.warning("UIEngine:debugMemory: found not registered page : " + nextEntry);
                    }
                    if (obj instanceof HamsterSession) {
                        HamsterSession session = (HamsterSession) obj;
                        if (!sessions.contains(session.getId())) {
                            LOG.warning("UIEngine:debugMemory: found an inconsistent session:  session is not in session map : " + nextEntry);
                        }
                    }
                }

//            if(obj instanceof DataModel ) {
//                DataModel d=(DataModel)obj;
//               
//            }
                if (obj instanceof Document) {
                    //check if obj is the same in the cache of the corresponding table.
                    Document e = (Document) obj;
                    if (!e.isIsDummy() && !e.isNew()) {
                        Document fromCache = e.getCollection().getById(e.getId());
                        if (fromCache != e) {
                            LOG.info("UIEngine:debugMemory: found entity which is not the same as in cache table:" + e.getCollection().getCollectionName() + "  id: " + e.getId() + " path: " + nextEntry);
                        }
                    }
                }
                if (obj instanceof UIEngine && obj != this) {
                    LOG.warning("UIEngine:debugMemory: found another UIEngine : " + nextEntry);
                }

                Class c = obj.getClass();
//            while (c != null) {
//                Field[] fields = c.getDeclaredFields();
                List<Field> fields = getAllFields(c);
                for (Field field : fields) {
                    field.setAccessible(true);
                    path.add(field.getName());
                    try {

                        Object value = field.get(obj);

                        if (value != null && value != path && value != checkedMap && !value.getClass().getName().startsWith("java.lang")) {
                            //	LOG.info("dm: " + c.getName() + " " + fields[i].getName() + " = " + value);
                            if (value instanceof Collection) {
                                for (Object v : (Collection) value) {
                                    if (v != null) {
                                        if (v != path && v != checkedMap && !checkedMap.containsKey(v)) {
                                            queue.add(new DebugPathEntry(v, field, nextEntry));
                                            checkedMap.put(v, v);
                                        }
                                    }
                                }
                                checkedMap.put(value, value);
                            } else if (!checkedMap.containsKey(value)) {
                                queue.add(new DebugPathEntry(value, field, nextEntry));
                                checkedMap.put(value, value);
                            }
                        }
                    } catch (IllegalArgumentException ex) {
                        LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
                    } catch (IllegalAccessException ex) {
                        LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
                    } finally {
                        path.removeLast();
                    }
                }
//                LOG.info("dm: superclass: "+c.getSuperclass()+" von "+c);
//                c = c.getSuperclass();
//            }
            }
        }
    }
    private static final HashMap<Class, List<Field>> fieldMap = new HashMap<Class, List<Field>>();

    private static final List<Field> getAllFields(Class<?> type) {
        List<Field> fields = fieldMap.get(type);
        if (fields != null) {
            return fields;
        }
        fields = new ArrayList<Field>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    public void checkSessionAndPage(HamsterPage page) {
        if (page.getSession() == null) {
            LOG.warning("page does not have a session ");
            return;
        }
        HamsterSession existingSession = sessions.get(page.getSession().getId());
        if (existingSession == null) {
            LOG.warning("checkSessionAndPage found an inconsistent session:  session is not in session map ");
            return;
        } else if (existingSession != page.getSession()) {
            LOG.warning("checkSessionAndPagefound an inconsistent session:  session id is in session map, but references a different session");
            page.setSession(existingSession);
            existingSession.addPage(page);
        }
        if (!page.getSession().getActivePages().contains(page)) {
            LOG.warning("checkSessionAndPage found an inconsistent page is not include in its own session");
            existingSession.addPage(page);
        }
    }

    public void clear() {
        UIContext.clear();
        ModificationManager.cleanUpThreadLocal();
    }

//    public synchronized void lockMap() {
//        while (mapLocked == true) {
//            try {
//                this.wait();
//            } catch (InterruptedException ex) {
//               LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
//            }
//        }
//        mapLocked = true;
//    }
//    public synchronized void unlockMap() {
//        mapLocked = false;
//        notify();
//    }
    public static void registerStaticAction(Action action, HamsterComponent comp) {
        HashMap map = null;
        map = (HashMap) staticActionComps.get(comp.getClass().getName());
        if (map == null) {
            map = new HashMap();
            staticActionComps.put(comp.getClass().getName(), map);
        }
        map.put(action.getStaticName(), action.getClass());
    }

    public static Action getStaticAction(String name, HamsterComponent comp) {
        HashMap map = null;
        map = (HashMap) staticActionComps.get(comp.getClass().getName());
        if (map != null) {
            Class c = (Class) map.get(name);

            if (c != null) {
                //support object orientation
                Class compClass = comp.getClass();
                while (compClass != null) {
                    if (c.getDeclaringClass().equals(compClass)) {
                        Action a = reconstructAction(c, comp, compClass);
                        if (a != null && a.getStaticName().equals(name)) {
                            return a;
                        }
                    }
                    compClass = compClass.getSuperclass();
                }
            }
        }
        return null;
    }

    public void addComponent(HamsterComponent comp) {
        //debug code
        if (!pages.containsKey(comp.getPage().getId())) {
            pages.put(comp.getPage().getId(), comp.getPage());
        }
        LOG.finest("adding component " + comp.getId() + " to page  " + comp.getPage().getId());
        comp.getPage().getComponentMap().put(comp.getId(), comp);
    }

    public void addPage(HamsterPage page) {
        if (!pages.containsKey(page.getId())) {
            pages.put(page.getId(), page);
        }
    }

    public void removeComponent(HamsterComponent comp) {
        LOG.finest("removing component " + comp.getId() + " to page  " + comp.getPage().getId());
        comp.getPage().getComponentMap().remove(comp.getId());
    }

    public static void addError(String error) {
        errors.add(error);
        LOG.log(Level.INFO, "Error: {0}", error);
    }

    public HttpServlet getServlet() {
        return CometProcessor.getServlet();
    }

    public void doRequest(final HttpServletRequest request,
            final HttpServletResponse response, final AsyncContext event) {
        long startTime = System.currentTimeMillis();
        executeSynchronously(new Runnable() {
            @Override
            public void run() {
                clear();
                setRequest(request);
                setResponse(response);
                synchronized (response) {
                    setEvent(event);
                    String result = doRequestInternal(request, response, event);
                    clear();
                    if (result != null) {
                        try {
                            HamsterPage page = getPageOfEvent(event);
                            if (page != null) {

                                try {
                                    page.getLock().tryLock(100, TimeUnit.MILLISECONDS);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, "could not aquire page lock after 100ms", ex);
                                    throw new RuntimeException("could not aquire page lock");
                                }
                                try {
                                    if (!response.isCommitted()) {
                                        writeResponse(request, response, result);
                                        event.complete();
                                    }
                                } finally {
                                    page.getLock().unlock();
                                }
                            } else {
                                writeResponse(request, response, result);
                                event.complete();

                            }
                        } catch (IOException ex) {
                            getLogger(UIEngine.class
                                    .getName()).
                                    log(Level.SEVERE, null, ex);
                        } finally {
                            clear();
                        }
                    }
                }
            }
        }, null, false);
        long endTime = System.currentTimeMillis();
        engine.reportRequestProcssing(startTime, endTime);
    }

    private void writeResponse(HttpServletRequest request, HttpServletResponse response, String content) throws IOException {
        if (acceptsGZip(request)) {
            response.addHeader("Content-Encoding", "gzip");
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            GZIPOutputStream os = new GZIPOutputStream(bs, true);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(content);
            writer.flush();
            os.flush();
            byte[] data = bs.toByteArray();
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } else {
            response.getWriter().write(content);
            response.getWriter().flush();
        }
    }

    private boolean acceptsGZip(HttpServletRequest httpRequest) {
        String acceptEncoding = httpRequest.getHeader("Accept-Encoding");
        return acceptEncoding != null && acceptEncoding.indexOf("gzip") != -1;
    }

    private String doRequestInternal(HttpServletRequest request,
            HttpServletResponse response, AsyncContext event) {

        if (!isInitilized()) {
            init();
        }
        // printMemoryStatus();
        response.setCharacterEncoding("UTF-8");
        try {
            request.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
        }
        if (!errors.isEmpty()) {
            // nur fehler ausgeben:
            String errorReport = "<h1>Error:</h1><h3>";
            Iterator iter = errors.iterator();
            while (iter.hasNext()) {
                errorReport += "<p>" + iter.next() + "</p>";
            }
            errorReport += "</h3>";
            return errorReport;
        }
        HamsterSession session = getOrCreateSession(request, response);
        UIContext.setUser(session.getUser());
        UIContext.setLocale(session.getLocale());
        UIContext.setParameterMap(request.getParameterMap());
        //LOG.info("request:answering2d");
        String context = "" + request.getServletPath();
        //LOG.info("context: " + context);
        String query = request.getQueryString();

        if (!context.equals("/") && (query == null || query.length() == 0 || query.
                startsWith("w"))) {
//			context = context.replaceAll("/", "");
            if (name != null && context.startsWith(name)) {
                context = context.substring(name.length());
            }
            String s = getSubPage(context, request, response, session);
            if (s != null && s.length() > 0) {
                if (s.startsWith("<")) {
                    return convertText(s);
                } else {
                    return s;
                }
            }
            if (s == null) {
                return null;
            }
        }
        if (query != null && !query.isEmpty()) {
            if (query.startsWith("S?")) {

                return convertText(reconstruct(new Reconstructor(query.
                        substring(2, query.
                                length()), this), request, response, session));
            }
            LinkedList<String> params = new LinkedList();
            StringTokenizer t = new StringTokenizer(query, "?");
            String command = t.nextToken();
            if (!t.hasMoreTokens()) {
                // Fehler in der URL
                return convertText(createNewPageInternal(session).toString());

            }
            String pageKey = t.nextToken();
            if (!t.hasMoreTokens()) {
                return convertText(createNewPageInternal(session).toString());
            }
            String componentKey = t.nextToken();
            while (t.hasMoreTokens()) {
                params.add(t.nextToken());
            }
            HamsterPage page = getPage(pageKey);
            if (page == null) {
                return "failed to lookup persisted session. Reload the page";
            }
            HamsterComponent comp = page.getComponentMap().get(componentKey);
            if (comp == null) {
                LOG.warning("component could not be found: " + componentKey + "  in page " + page.getId());
            }
            setPage(page);
            UIContext.setLocale(request.getLocales().nextElement());
            setUser(page.getUser());
            //           synchronized (page) {          
            page.markAsAlive();
            //page.setProcessing(true);
            page.checkUserAgent(request);
//            if (request.getMethod().equalsIgnoreCase("GET")) {
            registerConnection(page, event);
//            }
            if (command.equalsIgnoreCase("x") || command.
                    equalsIgnoreCase("a") || command.equalsIgnoreCase("c") || command.equalsIgnoreCase("z")) {
                return handleAjaxRequest(page, comp, command, params);
            } else {
                return handleWholePageRequests(page, comp, command, params);
            }
        }
        return convertText(createNewPageInternal(session));
    }

    public String handleAjaxRequest(HamsterPage page, HamsterComponent comp, String command, LinkedList<String> params) {

        try {
            if (!page.getLock().tryLock(10, TimeUnit.SECONDS)) {
                Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, "could not aquire page lock in time");
                return "n";
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, null, ex);
            return "n";
        }
        try {
            //set active flag for page
            Object active = UIContext.getParameterMap().get("active");
            if (active != null) {
                boolean isActive = Boolean.parseBoolean("" + ((String[]) active)[0]);
//                if(isActive != page.isUserActive()) {
                page.setUserActive(isActive);
//                }
            }

            if (command.equalsIgnoreCase("x") || command.
                    equalsIgnoreCase("a")) {

                // Die Seite fragt nach einem XML, das die Veraenderungen
                // beschreibt 
                if(Context.getTransaction().isDestroyed()) {
                    throw new RuntimeException("transaction cannot be already finished!");
                }
                comp.handleActions(params);
                if (page.getModificationManager().isEmpty() && comp == page) {
                    return null;
                }
                getTransactionManager().commit();
                return page.getModificationManager().getModificiationXML();

            } else if (command.equalsIgnoreCase("c")) {
                try {
                    // LOG.info("UIEngine: confirm ");
                    int id = parseInt(params.getFirst().toString());
                    ((HamsterPage) comp).getModificationManager().
                            confirmLastModificationXML(id);
                } catch (NumberFormatException ex) {
                    LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
                }
                return null;
            } else if (command.equalsIgnoreCase("z")) {	//for semi static links        
                try {
                    page.getModificationManager().setStaticRequest(true);
                    String spath = (String) params.getFirst();
                    Reconstructor2 rec = new Reconstructor2(spath, this);
                    rec.createRelativePath = false;
                    page.reconstruct(rec);
                } catch (Exception ex) {
                    LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
                }
                page.getModificationManager().setStaticRequest(false);
                getTransactionManager().commit();
                return page.getModificationManager().getModificiationXML();
            }
        } finally {
            page.getLock().unlock();
        }

        throw new IllegalArgumentException(command + " is not supported");
    }

    public String handleWholePageRequests(HamsterPage page, HamsterComponent comp, String command, LinkedList<String> params) {
        UIContext.setPage(comp.getPage());
        try {
            if (!page.getLock().tryLock(10, TimeUnit.SECONDS)) {
                Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, "could not aquire page lock in time");
                return "n";
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, null, ex);
            return "n";
        }
        try {
            page.fireOnShow();
            if (command.equalsIgnoreCase("m")) {
                try {
                    comp.handleActions(params);
                } catch (Exception ex) {
                    LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
                }
                getTransactionManager().commit();
                return convertText(comp.getPage().getHTMLCodeForPageCreation().toString());
            } else if (command.equalsIgnoreCase("k")) {
                // Seite ist im neuen Fenster/Tab
                // Klone Seite und fuehre Link auf neuer Seite aus
                HamsterComponent clone = (HamsterComponent) new CloneMap().
                        getClone(comp);
                try {
                    clone.getPage().getHTMLCode(); // muss gemacht werden,
                    // damit die actions frisch
                    // sind
                    clone.handleActions(params);
                } catch (Exception ex) {
                    LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
                }
                getTransactionManager().commit();
                return convertText(clone.getPage().getHTMLCode().toString());
            } else if (command.equalsIgnoreCase("u")) {
                // fuer FileUpload:

                try {
                    comp.handleActions(params);
                } catch (Exception ex) {
                    LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
                }
                getTransactionManager().commit();
                return convertText(comp.getIFrameHTMLCode());
            } else if (command.equalsIgnoreCase("i")) {
                // fuer iframes:
                getTransactionManager().commit();
                return convertText(comp.getIFrameHTMLCode());
            }
        } finally {
            page.getLock().unlock();
        }

        return "n";
    }

    public String createNewPageInternal(HamsterSession session) {
        HamsterPage page = createNewPage(session);

        page.getLock().lock();
        try {
            page.init();
            getTransactionManager().commit();
            page.getModificationManager().reset();
            return page.getHTMLCodeForPageCreation();
        } finally {
            page.getLock().unlock();
        }

    }

    public abstract HamsterPage createNewPage(HamsterSession session);

    protected String reconstruct(Reconstructor rec,
            HttpServletRequest request, HttpServletResponse response,
            HamsterSession session) {
        HamsterPage page = createNewPage(getOrCreateSession(request, response));
        page.getLock().lock();
        try {
            setPage(page);
            page.init();            
            page.getModificationManager().reset();
            page.getModificationManager().setStaticRequest(true);
            page.reconstruct(rec);
            page.getModificationManager().setStaticRequest(false);
            getTransactionManager().commit();
            return page.getHTMLCodeForPageCreation();
        } finally {
            page.getLock().unlock();
        }
    }

    public abstract boolean userExists(String userid, String userhash);

    public String getSubPage(String subPage, HttpServletRequest request,
            HttpServletResponse response, HamsterSession session) {

        //try to reconstruct page:
        return reconstruct(new Reconstructor2(subPage, this), request, response, session);

        //	LOG.info("dummy getSubPage: " + subPage);
        //	return "Dummy Sub Page " + subPage + " , " + request.getPathTranslated() + " , " + request.getQueryString() + "  ,  " + request.getServletPath();
    }

    public void logAction(HamsterComponent comp, Action action) {
    }

    /*
     * public void initLanguageManager(File path) { lanman = new
     * LanguageManager(path); }
     */

    public void printMemoryStatus() {
        gc();
        gc();
        gc();

        LOG.log(Level.INFO, "freier speicher: {0} von {1} frei  {2}% free total:{3}", new Object[]{getRuntime().
            freeMemory(), getRuntime().
            maxMemory(), getRuntime().
            freeMemory() / getRuntime().
            maxMemory() * 100.0f, getRuntime().totalMemory()});
    }

    public static String convertText(String xml) {
        String result = xml;
        try {
            String encoding = "UTF-8"; //"ISO-8859-1"
            //LOG.info("encoding als: "+request.getCharacterEncoding());

            result = decode(xml, encoding); //TODO: Warum geht UTF-8 nicht?
            //TODO write more performant replacement algo
            result = result.
                    replaceAll(decode("ä", encoding), "&auml;");
            result = result.
                    replaceAll(decode("ö", encoding), "&ouml;");
            result = result.
                    replaceAll(decode("ü", encoding), "&uuml;");
            result = result.
                    replaceAll(decode("Ä", encoding), "&Auml;");
            result = result.
                    replaceAll(decode("Ö", encoding), "&Ouml;");
            result = result.
                    replaceAll(decode("Ü", encoding), "&Uuml;");
            result = result.
                    replaceAll(decode("ß", encoding), "&szlig;");

            result = result.
                    replaceAll(decode("§", encoding), "&sect;");
            return result;
        } catch (UnsupportedEncodingException ex) {
            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
        }
        //            return xml;
        // result.replaceAll("ä","&auml;");
        // LOG.info("result: "+result);
        result = result.replaceAll("ä", "&auml;");
        result = result.replaceAll("ö", "&ouml;");
        result = result.replaceAll("ü", "&uuml;");
        result = result.replaceAll("Ä", "&Auml;");
        result = result.replaceAll("Ö", "&Ouml;");
        result = result.replaceAll("Ü", "&Uuml;");
        result = result.replaceAll("ß", "&szlig;");
        result = result.
                replaceAll("&", "&amp;");
        result = result.
                replaceAll("§", "&sect;");
        return result;
    }

    public Object prepareResume(UIEngine engine) {
        return engine;
    }

    protected Object writeReplace() {
        LOG.finest("Engine writeReplace");
        return placeHolder;
    }

    private final EnginePlaceHolder placeHolder = new EnginePlaceHolder();

    public static class EnginePlaceHolder implements Serializable {

        protected Object readResolve() throws ObjectStreamException {
            return getEngine();
        }
    }

    public synchronized void persistPage(HamsterPage p) {
        try {
            if (!p.getLock().tryLock(10, TimeUnit.SECONDS)) {
                Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, "Could not aquire page lock after 10 seconds");
                return;
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        try {
            if (!p.isDestroyed()) {
                //don't persist for not login users
                if (p.getUser() != Users.DEFAULT_USER && p.getUser() != null) {
                    String fileName = "/pages/" + p.getId();
                    if (persistedPages.checkAndCleanup(p.getUser().getId(), fileName)) {
                        p.persist(gridFS.openUploadStream(fileName));
                    } else {
                        LOG.log(Level.WARNING, "Did not persist page for " + p.getUser().getId() + "  user has persisted to many pages in current time");
                    }
                }
            }
        } finally {
            p.getLock().unlock();
        }
    }

//    public synchronized void persistPage(HamsterPage p) {       
//       for(int i=1; i < 100; i++) {   
//         //String id=p.getId();
//         System.out.println("peristing page "+p.getId());
//         ByteArrayOutputStream o=new ByteArrayOutputStream();
//         p.persist(o);
//         p.destroy();
//         System.out.println("page size:  "+o.toByteArray().length);
//         p=resume(new ByteArrayInputStream(o.toByteArray()), this);
//       }
//    }
    public synchronized HamsterPage resumePage(String pageId) {
        String fileName = "/pages/" + pageId;
        LOG.log(Level.INFO, "resuming page: {0}", fileName);

        HamsterPage page = null;
        try {

            page = resume(gridFS.openDownloadStreamByName(fileName), this);
        } catch (Exception ex) {
            File sessionFile = new File(System.getProperty("catalina.base") + "/logs/failedResume" + pageId);
            LOG.log(Level.SEVERE, "error while restoring page, storing backup: " + sessionFile, ex);
            OutputStream out = null;
            try {
                out = new FileOutputStream(sessionFile);
                gridFS.downloadToStreamByName(fileName, out);
                //throw ex;
            } catch (IOException ex1) {
                Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, null, ex1);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex1) {
                        Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
            }
        }
        if (page == null) {
            //try to delete broken stored page:

            LOG.log(Level.WARNING, "page can not be restored  {0}  {1}", new Object[]{fileName, pageId});
        } else {
            LOG.log(Level.INFO, "resumed page: {0}   {1}", new Object[]{fileName, page.
                getId()});
        }
        gridFS.find(Filters.eq("filename", fileName)).forEach(new Block<GridFSFile>() {
            @Override
            public void apply(GridFSFile file) {
                gridFS.delete(file.getObjectId());
            }
        });
        return page;
    }

    public HamsterPage getPage(String pageId) {
//        LOG.info("getPage: "+pageId);
        HamsterPage page = pages.get(pageId);
        if (page == null) {
            page = resumePage(pageId);
            if (page != null) {
                pages.put(page.getId(), page);
                addUserPage(page.getUser().getId(), page);
            } else {
                LOG.log(Level.WARNING, "resuming page{0} failed", pageId);
            }
        }
        return page;
    }

    public void removeSession(HamsterSession session) {
        if (sessions.get(session.getId()) == session) {
            sessions.remove(session.getId());
        }
    }

    public HamsterSession createSession(HttpServletRequest request, HttpServletResponse response, String sessionId) {
        HamsterSession session = new HamsterSession(this);
        if (sessionId != null) {
            session.setId(sessionId);
        }
        UIContext.setSession(session);

        Cookie sessionCookie = new Cookie("sid", "" + session.getId()); // kuchen backen :-)
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);
        session.setLocale(request.getLocales().nextElement());
        sessions.put(session.getId(), session);
        return session;
    }

    public HamsterSession getSession(String id) {
        return sessions.get(id);
    }

    public void putSession(HamsterSession session) {
        sessions.put(session.getId(), session);
    }

    public HamsterSession getOrCreateSession(HttpServletRequest request, HttpServletResponse response) {
        if (UIContext.getSession() != null) {
            return UIContext.getSession();
        }
        Cookie[] cookies = request.getCookies();
        String sessionId = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("sid")) {
                    sessionId = cookie.getValue();
                }
            }
        }
        HamsterSession session = null;
        if (sessionId != null) {
            session = sessions.get(sessionId);
            if (session != null) {
                return session;
            } else {
                return createSession(request, response, sessionId);
            }
        }
        if (session == null) {
            return createSession(request, response, null);
        } else {
            return session;
        }
    }

    public abstract Locale getUserLocale(Document user);

    public static UIEngine loadEngine(String engineClassName) {
        try {
            Class c = Thread.currentThread().getContextClassLoader().loadClass(engineClassName);
            return (UIEngine) c.newInstance();
        } catch (Exception ex) {
            Logger.getLogger(UIEngine.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    private static final Logger LOG = getLogger(UIEngine.class
            .getName());
}
