package com.kaibla.hamster.persistence.events;

import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.base.DataModel;
import com.kaibla.hamster.base.AbstractListenerOwner;
import com.kaibla.hamster.base.HamsterEngine;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author kai
 */
public abstract class EventProxyFactory<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    HashMap<T, EventProxy> proxies;
    private static HashMap<String, EventProxyFactory> instances=new HashMap<>();

    public EventProxyFactory() {
        this.proxies = new HashMap();
        instances.put(getClass().getName(), this);
    }

    public abstract void createProxy(T key, EventProxy proxy);

    public synchronized DataModel getProxy(final T key,HamsterEngine engine) {        
        EventProxy proxy = proxies.get(key);
        if (proxy == null) {
            proxy = new EventProxy(key, this, engine);
            createProxy(key, proxy);
            engine.addModel(proxy);
            proxies.put(key,proxy);
        }
        return proxy;
    }
    
    public void attachProxy(final T key, AbstractListenerOwner cmp) {
        DataModel p = getProxy(key, cmp.getListenerContainer().getEngine());
        cmp.acquireDataModel(p);
        
    }

    protected synchronized void remove(T key) {
        proxies.remove(key);
    }
    
    protected Object writeReplace() {
        return new PlaceHolder(getClass().getName());
    }

    public static class PlaceHolder implements Serializable {
        String  className;

        public PlaceHolder(String className) {
            this.className = className;
        }
      
        protected Object readResolve() throws ObjectStreamException {            
            return instances.get(className);
        }
    }
}
