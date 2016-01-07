/*
 * DataEvent.java Created on 18. Februar 2007, 17:08
 */
package com.kaibla.hamster.base;

import static java.util.Collections.synchronizedSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * @author Kai Orend
 */
public class DataEvent {

    private final Set firedOn;
    private final DataModel source;

    /**
     * Creates a new instance of DataEvent
     */
    public DataEvent(DataModel source) {
        this.source = source;
        firedOn = synchronizedSet(new HashSet());
    }

    public boolean hasBeenFiredOn(Object o) {
        if (firedOn.contains(o)) {
            return true;
        } else {
            firedOn.add(o);
            return false;
        }
    }

    public void clearHistory() {
        firedOn.clear();
    }

    public DataModel getSource() {
        return source;
    }
    private static final Logger LOG = getLogger(DataEvent.class.getName());
}
