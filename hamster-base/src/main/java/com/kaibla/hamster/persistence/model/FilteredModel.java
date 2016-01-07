package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.base.ChangedListener;
import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.DataModel;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.events.DataObjectChangedEvent;
import com.kaibla.hamster.persistence.attribute.Attribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author kai
 */
public class FilteredModel<T> extends DataModel {

    private static final long serialVersionUID = 1L;
    private final ConcurrentHashMap<T, Set<ChangedListener>> criteriaListeners;
    private final ConcurrentHashMap<ChangedListener, Set<T>> reverseMap;

    public FilteredModel(HamsterEngine engine) {
        super(engine);
        criteriaListeners = new ConcurrentHashMap<T, Set<ChangedListener>>();
        reverseMap = new ConcurrentHashMap<ChangedListener, Set<T>>();
    }

    public  void addChangedListener(ChangedListener listener, T... attrs) {      
        ArrayList<T> criterias = new ArrayList();
        Set<T> criteriaSet = getReverseSet(listener);
        for (T attr : attrs) {
            getCriteriaListeners(attr).add(listener);
            criteriaSet.add(attr);
            criterias.add(attr);

        }
//        if (listener instanceof HamsterComponent) {
////            persistFilterResgistration((HamsterComponent) listener, criterias);
//            ((HamsterComponent) listener).addEventFilter(new AttributeFilter(this, (List<Attribute>) criterias));
//        }
    }
    
//    public abstract void persistFilterResgistration(HamsterComponent comp, ArrayList<T> criterias);

    @Override
    public  Collection<ChangedListener> getFilteredListener(DataEvent e) {
        if (e instanceof DataObjectChangedEvent) {
            HashSet<ChangedListener> listeners = new HashSet();
            DataObjectChangedEvent ch = (DataObjectChangedEvent) e;
            for (Attribute attr : ch.getChangedAttributes()) {
                Set<ChangedListener> l = criteriaListeners.get(attr);
                if (l != null) {
                    listeners.addAll(l);
                }
            }
            return listeners;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public  Collection<ChangedListener> getFilteredListener(T object) {
        return criteriaListeners.get(object);
    }

    @Override
    public void removeChangedListener(ChangedListener listener) {
        Set<T> criterias = reverseMap.remove(listener);
        if (criterias != null) {
            for (T criteria : criterias) {
                Set<ChangedListener> listeners = criteriaListeners.get(criteria);
                if (listeners != null) {
                    listeners.remove(listener);
                    if (listeners.isEmpty()) {
                        criteriaListeners.remove(criteria);
                    }
                }
            }
        }
        super.removeChangedListener(listener);
    }

    @Override
    public  boolean hasListeners() {
        return !criteriaListeners.isEmpty() || super.hasListeners();
    }
    
    private Set crateConcurrentHashSet() {
        return Collections.newSetFromMap(new ConcurrentHashMap());
    }

    private  Set<ChangedListener> getCriteriaListeners(T attr) {
        Set<ChangedListener> listeners = criteriaListeners.get(attr);
        if (listeners == null) {
            listeners = crateConcurrentHashSet();
            criteriaListeners.put(attr, listeners);
        }
        return listeners;
    }

    private Set<T> getReverseSet(ChangedListener l) {
        Set<T> criterias = reverseMap.get(l);
        if (criterias == null) {
            criterias = crateConcurrentHashSet();
            reverseMap.put(l, criterias);
        }
        return criterias;
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
//        cleanUp(reverseMap.keySet());
        Set<Entry<ChangedListener, Set<T>>> entries=reverseMap.entrySet();
        Iterator<Entry<ChangedListener, Set<T>>> iter = entries.iterator();
        LinkedList<ChangedListener> removed=new LinkedList();
        while(iter.hasNext()) {
            Entry<ChangedListener, Set<T>> entry = iter.next();
            ChangedListener l = entry.getKey();
            if(listenerIsDestroyed(l)) {               
                removed.add(l);                
            }
        }
        for(ChangedListener l : removed) {
            removeChangedListener(l);
        }
//        Iterator<HashSet<ChangedListener>> setIter = new ArrayList(criteriaListener.values()).iterator();
//        while(setIter.hasNext()) {
//            HashSet<ChangedListener> set= setIter.next();                     
//            cleanUp(set);            
////            if(set.isEmpty()) {
////                setIter.remove();
////            }
//        }  
        if(!hasListeners()) {
            destroy();
        }
    }
    
}
