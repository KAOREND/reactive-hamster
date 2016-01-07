package com.kaibla.hamster.base;

import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.model.AttributeFilter;
import com.kaibla.hamster.persistence.model.FilteredModel;
import com.kaibla.hamster.persistence.model.QueryFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kai Orend
 */
public abstract class AbstractListenerOwner implements ChangedListener {

    private Set<DataModel> eventSources = new HashSet();
    private Set<DataModel> holdedSources = new HashSet();
    private Set<AttributeFilter> filteredEventSources = new HashSet();
    private Set<QueryFilter> queryEventSources = new HashSet();
    private AbstractListenerContainer listenerContainer;
    
    public AbstractListenerOwner(AbstractListenerContainer container) {
        setListenerContainer(listenerContainer);
    }
    
    public AbstractListenerOwner() {
        
    }

    public AbstractListenerContainer getListenerContainer() {
        return listenerContainer;
    }

    public void setListenerContainer(AbstractListenerContainer listenerContainer) {
        this.listenerContainer = listenerContainer;
        eventSources = new HashSet<DataModel>();
    }

    public void addEventSource(DataModel model) {
        try {
            getListenerContainer().getLock().tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(AbstractListenerOwner.class.getName()).log(Level.SEVERE, "could not aquire page lock after 10 seconds ", ex);
            throw new RuntimeException("could not aquire page lock");
        }
        try {
            eventSources.add(model);
        } finally {
            getListenerContainer().getLock().unlock();
        }
    }

    public void addEventHolder(DataModel model) {
        try {
            getListenerContainer().getLock().tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(AbstractListenerOwner.class.getName()).log(Level.SEVERE, "could not aquire page lock after 10 seconds ", ex);
            throw new RuntimeException("could not aquire page lock");
        }
        try {
            holdedSources.add(model);
        } finally {
            getListenerContainer().getLock().unlock();
        }
    }

    public void addEventFilter(AttributeFilter model) {
        try {
            getListenerContainer().getLock().tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(AbstractListenerOwner.class.getName()).log(Level.SEVERE, "could not aquire page lock after 10 seconds ", ex);
            throw new RuntimeException("could not aquire page lock");
        }
        try {
            filteredEventSources.add(model);
        } finally {
            getListenerContainer().getLock().unlock();
        }
    }

    public void addEventFilter(QueryFilter model) {
        try {
            getListenerContainer().getLock().tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(AbstractListenerOwner.class.getName()).log(Level.SEVERE, "could not aquire page lock after 10 seconds ", ex);
            throw new RuntimeException("could not aquire page lock");
        }
        try {
            queryEventSources.add(model);
        } finally {
            getListenerContainer().getLock().unlock();
        }
    }

    public void afterResume() {
        for (DataModel d : new ArrayList<DataModel>(eventSources)) {
            if (d != null) {
                acquireDataModel(d);
            }
        }
        for (DataModel d : new ArrayList<DataModel>(holdedSources)) {
            if (d != null) {
                holdDataModel(d);
            }
        }
        for (AttributeFilter f : new ArrayList<AttributeFilter>(filteredEventSources)) {

            List<Attribute> attributes = f.getCriteria();

            Attribute[] attributeArray = new Attribute[attributes.size()];
            int i = 0;
            for (Object attr : f.getCriteria()) {
                attributeArray[i] = (Attribute) attr;
                i++;
            }
            this.acquireDataModel(f.getModel(), attributeArray);
        }

        for (QueryFilter f : new ArrayList<QueryFilter>(queryEventSources)) {
            f.getModel().addChangedListener(this, f.getQuery());
        }
    }

    public void acquireDataModel(DataModel model) {
        model.addChangedListener(this);
    }

    public void holdDataModel(DataModel model) {
        model.addHolder(this);
    }

    public void acquireDataModel(FilteredModel model, Attribute... attrs) {
        model.addChangedListener(this, attrs);
    }

    public void destroy() {

        for (DataModel model : eventSources) {
            model.removeChangedListener(this);
        }
        for (DataModel model : holdedSources) {
            model.removeChangedListener(this);
        }
        for (AttributeFilter filteredModel : filteredEventSources) {
            filteredModel.getModel().removeChangedListener(this);
        }
        for (QueryFilter filter : queryEventSources) {
            filter.getModel().removeChangedListener(this);
        }
    }

    public void copyFrom(AbstractListenerOwner orig, Map map) {
        map.put(orig, this);
        eventSources.clear();
        holdedSources.clear();
        filteredEventSources.clear();
        queryEventSources.clear();
        for (DataModel model : orig.eventSources) {
            acquireDataModel(model);
        }
        for (DataModel model : orig.holdedSources) {
            holdDataModel(model);
        }
        for (AttributeFilter f : orig.filteredEventSources) {
            this.acquireDataModel(f.getModel(), (Attribute[]) f.getCriteria().toArray());
        }
        for (QueryFilter f : orig.queryEventSources) {
            f.getModel().addChangedListener(this, f.getQuery());
        }

    }

}
