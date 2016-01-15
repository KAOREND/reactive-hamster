/*
 * ComponentManager.java Created on 18. Februar 2007, 17:33
 */
package com.kaibla.hamster.base;

import com.kaibla.hamster.monitoring.AutomaticMonitoring;
import com.kaibla.hamster.persistence.events.EventQueue;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import java.io.Serializable;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.lang.Thread.yield;
import static java.util.Collections.newSetFromMap;
import static java.util.Collections.synchronizedList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newFixedThreadPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * @author Kai Orend
 */
public abstract class HamsterEngine implements Runnable, Serializable {
    private static final long serialVersionUID = 1L;

    private final List threads = synchronizedList(new LinkedList());
    private final Set<DataModel> models = newSetFromMap(new ConcurrentHashMap());

    private final ExecutorService executer = newFixedThreadPool(100);

    private boolean destroyed = false;
    private boolean init = false;
    private static HamsterEngine engine = null;
    private EventQueue  eventQueue = null;

    /**
     * Thread Local for collecting all pages which need to be updated after this session.
     */
    ThreadLocal<HashSet<AbstractListenerContainer>> changedPages = new ThreadLocal<HashSet<AbstractListenerContainer>>();

    public synchronized void init() {
        init = true;
        engine = this;
    }
    
     public void initDB(MongoDatabase db) {
        eventQueue = new EventQueue(db, this);
    }

    public EventQueue getEventQueue() {
        return eventQueue;
    }

    public boolean isInitilized() {
        return init;
    }

    public static HamsterEngine getEngine() {
        return engine;
    }

    public void addThread(Thread t) {
        threads.add(t);
    }

    public void addModel(DataModel model) {
        models.add(model);
    }

    public void removeModel(DataModel model) {
        models.remove(model);
    }
    
    public int countModels() {
        return models.size();
    }

    /**
     * Notifies the Engine that modifications in the page can be written
     *
     * @param page
     */
    public void updatePage(final AbstractListenerContainer page) {
        HashSet<AbstractListenerContainer> changes = changedPages.get();
        if (changes == null) {
            changes = new HashSet<AbstractListenerContainer>();
            changedPages.set(changes);
        }
        changes.add(page);
    }

    /**
     * The page which were affected during the last request
     */
    public void updateOtherPages() {
        HashSet<AbstractListenerContainer> changes = changedPages.get();
        changedPages.remove();
        if (changes != null) {
            try {
                for (final AbstractListenerContainer page : changes) {
                    if (page.hasPendingTodos()) {
                        updateSinglePage(page);
                    }
                }
            } finally {
                changes.clear();
            }
        }
    }

    public void updateSinglePage(final AbstractListenerContainer page) {
        executer.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    //try to get lock and backout if not possible in order to avoid deadlocks
                    if (!page.getLock().tryLock(100, TimeUnit.MILLISECONDS)) {
                        //put it back into the execution queue if we cannot get the lock yet
                        updateSinglePage(page);
                        return;
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(HamsterEngine.class.getName()).log(Level.SEVERE, null, ex);
                }

                try {
                    for (Object runnable : page.flushPending()) {
                        try {
                            executeSynchronously((Runnable)runnable, page, true);
                        } catch (Exception ex) {
                            getLogger(HamsterEngine.class.
                                    getName()).
                                    log(Level.SEVERE, null, ex);
                        }
                    }
                    updateListenerContainer(page);
                } finally {
                    page.getLock().unlock();
                }
            }
        });
    }

    public void executeSynchronouslyIfInSamePage(final Runnable runnable, final AbstractListenerContainer page) {
        if (page != null && page == Context.getListenerContainer()) {
            runnable.run();
        } else {
            execute(runnable, page);
        }
    }

    public void execute(final Runnable runnable, final AbstractListenerContainer page) {
        if (page != null) {
            page.addToPendingQueue(runnable);
            updatePage(page);
        } else {
            executer.execute(new Runnable() {
                @Override
                public void run() {
                    executeSynchronously(runnable, page, true);
                }
            });
        }
    }

    public void executeSynchronously(final Runnable runnable, final AbstractListenerContainer page, final boolean allowReschedule) {
        AutomaticMonitoring.run(new Runnable() {
            @Override
            public void run() {
                if (page != null) {
                    try {
                        if (!page.getLock().tryLock(100, TimeUnit.MILLISECONDS)) {
                            Logger.getLogger(HamsterEngine.class.getName()).log(Level.SEVERE, "Could not aquire page lock after 100ms");
                            //reschedule this, try it again later
                            if (allowReschedule) {
                                Logger.getLogger(HamsterEngine.class.getName()).log(Level.WARNING, "rescheduling request");
                                execute(runnable, page);
                                updateOtherPages();
                            } else {
                                Logger.getLogger(HamsterEngine.class.getName()).log(Level.SEVERE, "Could not reschedule request");
                            }
                            return;
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(HamsterEngine.class.getName()).log(Level.SEVERE, null, ex);
                        return;
                    }
                    try {
                        executeSynchronouslyInternal(runnable, page);
                    } finally {
                        page.getLock().unlock();
                    }
                } else {
                    executeSynchronouslyInternal(runnable, null);
                }
            }
        }, 5000, "");
    }

    protected void executeSynchronouslyInternal(final Runnable runnable, final AbstractListenerContainer page) {
        try {
            //start transaction
            Context.clear();           
            AutomaticMonitoring.run(runnable, 5000, "");
            //finish transaction                        
            updateOtherPages();
            Context.clear();
        } finally {
            Context.clear();
        }
    }
    
        @Override
    public void run() {
        while (!isDestroyed()) {
            try {
                sleep(20000);
                if (isDestroyed()) {
                    return;
                }
                boolean cleanedUpSomething = false;
                long time = currentTimeMillis();
                
                Iterator<DataModel> modelsIterator = models.iterator();
                while (modelsIterator.hasNext()) {
                    DataModel model = modelsIterator.next();
                    try {
                        model.cleanUp();
                    } catch (Exception ex) {
                        LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
                    }
                    if (model.isDestroyed()) {
                        modelsIterator.remove();
                        cleanedUpSomething = true;
                    }
                    yield();
                }
                if(periodicCleanup())cleanedUpSomething=true;
                //a good time to free some memory
                //System.gc();
                LOG.info("time for cleaning inlcuding gc " + (System.currentTimeMillis() - time));
//                if (cleanedUpSomething) {
//                    debugMemoryAndThreadLocals(this);
//                }

            } catch (Throwable ex) {
                LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
            } 
//             unlockMap();
        }

    }
    
    /**
     * This method is called by the cleanup thread. Override this if you want to do additional cleanups.
     * @return true if the method cleaned up something
     */
    public boolean periodicCleanup() {
        return false;
    }

    public void destroy() {
        destroyed = true;
        LOG.info("stopping all HamsterThreads");
        Iterator iter = threads.iterator();
        while (iter.hasNext()) {
            Thread t = (Thread) iter.next();
            t.interrupt();
        }
        LOG.info("Stopping executer");
        executer.shutdown();
        engine=null;
        LOG.info("Engine stopped");
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public abstract void updateListenerContainer(AbstractListenerContainer listenerContainer);

    private static final Logger LOG = getLogger(HamsterEngine.class
            .getName());
}
