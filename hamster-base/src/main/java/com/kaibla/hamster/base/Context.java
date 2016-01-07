package com.kaibla.hamster.base;

import com.kaibla.hamster.persistence.model.Document;
import java.util.HashMap;
import java.util.Locale;
import static java.util.Locale.getDefault;
import java.util.Map;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author kai
 */
public class Context {
    private final static ThreadLocal<InternalContext> contextMap = new ThreadLocal<InternalContext>();

    protected static InternalContext getContext() {
        InternalContext result = contextMap.get();
        if (result == null) {
            result = new InternalContext();
            contextMap.set(result);
        }
        return result;
    }

    public static void clear() {
        contextMap.remove();
    }

    public static void setListenerContainer(AbstractListenerContainer listenerContainer) {
        getContext().listenerContainer = listenerContainer;
    }
    
    public static AbstractListenerContainer getListenerContainer() {
        return getContext().listenerContainer;
    }
    
    
    private static class InternalContext {
        AbstractListenerContainer listenerContainer;        
    }
    
    private static final Logger LOG = getLogger(Context.class.getName());
}
