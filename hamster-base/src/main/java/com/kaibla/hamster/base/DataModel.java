/*
 * DatabaseObject.java Created on 18. Februar 2007, 17:06
 */
package com.kaibla.hamster.base;


import com.kaibla.hamster.persistence.model.ListModel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * @author Kai Orend
 */
public class DataModel implements Serializable,Resumable {

    private Set<ChangedListener> changedListener = null;
    //listener, which do not want to be called, but do not want this to garbage collected
    private Set<ChangedListener> holders = null;
   
    boolean destroyed = false;
    HamsterEngine engine;

    /**
     * Creates a new instance of DatabaseObject
     */
    public DataModel(HamsterEngine engine) {
        changedListener = Collections.newSetFromMap(new ConcurrentHashMap<ChangedListener, Boolean>());
        holders = Collections.newSetFromMap(new ConcurrentHashMap<ChangedListener, Boolean>());
        this.engine = engine;
        engine.addModel(this);
    }

    public boolean hasListeners() {
        return !changedListener.isEmpty() || !holders.isEmpty();
    }

    public HamsterEngine getEngine() {
        return engine;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void destroy() {
        destroyed = true;
    }

    public Collection<ChangedListener> getFilteredListener(final DataEvent e) {
        return Collections.EMPTY_LIST;
    }

    public void addChangedListener(ChangedListener listener) {
        if (changedListener.add(listener)) {
            if (listener instanceof AbstractListenerOwner) {
                AbstractListenerOwner comp = (AbstractListenerOwner) listener;
                comp.addEventSource(this);
            }
        }
        destroyed = false;
    }

    public void addHolder(ChangedListener listener) {

        if (holders.add(listener)) {
            if (listener instanceof AbstractListenerOwner) {
                ((AbstractListenerOwner) listener).addEventHolder(this);
            }
        }
        destroyed = false;

    }

    public void removeChangedListener(ChangedListener listener) {
        changedListener.remove(listener);
        holders.remove(listener);

    }

    public void fireChangedEvent(final DataEvent e) {
        ArrayList copy = null;

        copy = new ArrayList(changedListener.size());
        copy.addAll(changedListener);

        copy.addAll(getFilteredListener(e));

        Iterator iter = copy.iterator();
        while (iter.hasNext()) {
            final ChangedListener listener = (ChangedListener) iter.next();
            if (listener instanceof AbstractListenerOwner || listener instanceof ListModel) {
                if (!listener.isDestroyed()) {
                    AbstractListenerContainer p = null;
                    if (listener instanceof AbstractListenerOwner) {
                        p = ((AbstractListenerOwner) listener).getListenerContainer();
                    } else {
                        p = ((ListModel) listener).getOwner().getListenerContainer();
                    }

                    final AbstractListenerContainer page = p;
                    if (Context.getListenerContainer() == page) {
                        if (!e.hasBeenFiredOn(listener)) {
                            listener.dataChanged(e);
                        }
                    } else {
                        page.getEngine().execute(new Runnable() {
                            @Override
                            public void run() {
                                if (!e.hasBeenFiredOn(listener)) {
                                    listener.dataChanged(e);
                                }
                            }
                        }, page);
                    }
                }
            } else {
                if (!e.hasBeenFiredOn(listener)) {
                    listener.dataChanged(e);
                }
            }
        }
    }
    
    

//    public void setNoEvents(boolean noEvents) {
//        this.noEvents = noEvents;
//    }
//
//    public boolean isNoEvents() {
//        return noEvents;
//    }

    /*
     * Is called by the cleaner thread to remove dead listener entries. Should
     * be overriden by DataModels which manage listeners themselves.
     */
    public synchronized void cleanUp() {
        cleanUp(changedListener);
        cleanUp(holders);
    }

    protected synchronized void cleanUp(Collection listeners) {
        synchronized (listeners) {
            Iterator iter = new ArrayList(listeners).iterator();
            while (iter.hasNext()) {
                ChangedListener listener = (ChangedListener) iter.next();
                if (listenerIsDestroyed(listener)) {
                    iter.remove();
                    removeChangedListener(listener);
                }
            }

        }
    }

    protected boolean listenerIsDestroyed(ChangedListener listener) {
        if (listener instanceof AbstractListenerOwner) {
            AbstractListenerOwner comp = (AbstractListenerOwner) listener;
            if (comp.isDestroyed()) {
//		    LOG.info("AbstractListenerOwner " + comp.getClass().getName() + " is destroyed but registered on: " + this.getClass().getName());
                return true;
            }
        } else if (listener instanceof DataModel) {
            DataModel dataModel = (DataModel) listener;
            if (dataModel.isDestroyed() || !dataModel.hasListeners()) {
                dataModel.destroy();
                return true;
            }
        } else if (listener.isDestroyed()) {
            return true;
        }
        return false;
    }
    private static final Logger LOG = getLogger(DataModel.class.getName());

    @Override
    public void resume() {
        engine.addModel(this);
    }
}
