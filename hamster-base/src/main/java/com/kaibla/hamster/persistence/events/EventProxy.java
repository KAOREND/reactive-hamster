package com.kaibla.hamster.persistence.events;

import com.kaibla.hamster.base.ChangedListener;
import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.DataModel;
import com.kaibla.hamster.base.AbstractListenerOwner;
import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.base.HamsterEngine;
import static com.kaibla.hamster.persistence.model.DocumentCollection.getByName;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashSet;

/**
 *
 * @author kai
 */
public class EventProxy<T> extends DataModel implements ChangedListener {

    private static final long serialVersionUID = 1L;
    private T key;
    private EventProxyFactory factory;

    public EventProxy(T key, EventProxyFactory factory, HamsterEngine engine) {
        super(engine);
        this.key = key;
        this.factory = factory;
    }

    @Override
    public void destroy() {
        super.destroy();
        factory.remove(key);
    }

    @Override
    public void dataChanged(final DataEvent e) {
        fireChangedEvent(e);
    }

    @Override
    public void removeChangedListener(ChangedListener listener) {
        super.removeChangedListener(listener); 
        if(!hasListeners()) {
            destroy();
        }
    }
    
    

    protected Object writeReplace() {
        return new PlaceHolder(key,factory);
    }

    public static class PlaceHolder implements Serializable {
        Object key;
        private EventProxyFactory factory;

        public PlaceHolder(Object key, EventProxyFactory factory) {
            this.key = key;
            this.factory = factory;
        }
      
        protected Object readResolve() throws ObjectStreamException {            
            return factory.getProxy(key, HamsterEngine.getEngine());
        }
    }
}
