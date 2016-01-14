package com.kaibla.hamster.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author kai
 */
public abstract class AbstractListenerContainer<T> extends AbstractListenerOwner implements Resumable {

    private transient Lock lock = new ReentrantLock();
    private transient List<Runnable> pendingQueue = new LinkedList();
    private HamsterEngine engine;
    private T owner;

    public AbstractListenerContainer(HamsterEngine engine, T owner) {
        super(null);
        setListenerContainer(this);
        this.engine = engine;
        this.owner = owner;

    }

    public T getOwner() {
        return owner;
    }

    public void interrupt() {
        engine.updatePage(this);
    }

    protected void addToPendingQueue(Runnable runnable) {
        synchronized (this) {
            if (pendingQueue == null) {
                pendingQueue = new LinkedList();
            }
        }
        synchronized (pendingQueue) {
            pendingQueue.add(runnable);
        }
    }

    protected Collection<Runnable> flushPending() {
        synchronized (pendingQueue) {
            Collection<Runnable> result = new ArrayList<>(pendingQueue);
            pendingQueue.clear();
            return result;
        }
    }

    protected boolean hasPendingTodos() {
        synchronized (pendingQueue) {
            return !pendingQueue.isEmpty();
        }
    }

    public void setEngine(HamsterEngine engine) {
        this.engine = engine;
    }

    public HamsterEngine getEngine() {
        return engine;
    }

    public Lock getLock() {
        if (lock == null) {
            lock = new ReentrantLock();
        }
        return lock;
    }

    @Override
    public void resume() {
        pendingQueue = new LinkedList();
    }

}
