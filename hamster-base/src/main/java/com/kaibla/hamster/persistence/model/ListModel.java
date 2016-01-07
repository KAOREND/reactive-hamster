package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.events.DataObjectCreatedEvent;
import com.kaibla.hamster.persistence.events.ListChangedEvent;
import com.kaibla.hamster.persistence.events.DataObjectDeletedEvent;
import com.kaibla.hamster.persistence.events.MongoEvent;
import com.kaibla.hamster.base.ChangedListener;
import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.DataModel;
import com.kaibla.hamster.base.AbstractListenerOwner;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.base.Resumable;
import java.util.SortedSet;

/**
 *
 * @author kai
 */
public abstract class ListModel extends DataModel implements Resumable {

    private DataModel model;
    private ChangedListener tableListener;
    private final ListModel self = this;
    private boolean tableEvents = true;
    private AbstractListenerOwner owner;
    protected boolean unDestroyable = false;

    public ListModel(AbstractListenerOwner comp) {
        super(comp.getListenerContainer().getEngine());
        owner = comp;
    }

    public ListModel(AbstractListenerOwner comp, DataModel model) {
        super(comp.getListenerContainer().getEngine());
        owner = comp;
        setParentModel(model);
    }

    public ListModel(AbstractListenerOwner comp, DataModel model, boolean tableEvents) {
        super((comp == null) ? model.getEngine() : comp.getListenerContainer().getEngine());
        owner = comp;
        this.tableEvents = tableEvents;
        setParentModel(model);

    }

    public ListModel(HamsterEngine engine, DataModel model, boolean tableEvents) {
        super(engine);
        this.tableEvents = tableEvents;
        setParentModel(model);
        unDestroyable = true;
    }

    public AbstractListenerOwner getOwner() {
        return owner;
    }

    public void setParentModel(final DataModel model) {
        this.model = model;
        tableListener = new ChangedListener() {
            @Override
            public void dataChanged(DataEvent e) {
                //if (!e.hasBeenFiredOn(this)) {
                if (e instanceof DataObjectCreatedEvent) {
                    DataObjectCreatedEvent ec = (DataObjectCreatedEvent) e;
                    if (contains(ec.getMongoObject())) {
                        fireChangedEvent(e);
                    }
                } else if (e instanceof DataObjectDeletedEvent) {
                    DataObjectDeletedEvent ec = (DataObjectDeletedEvent) e;
                    if (contains(ec.getMongoObject())) {                        
                        fireChangedEvent(e);
                    }
                } else if (e instanceof ListChangedEvent) {
                    ListChangedEvent ec = (ListChangedEvent) e;
                    if (contains(ec.getDatabaseObject())) {
                        fireChangedEvent(e);
                        ec.addFireAfterCommit(self);
                    }
                } else if (e instanceof MongoEvent) {
                    MongoEvent ev = (MongoEvent) e;
                   // if (contains(ev.getMongoObject())) {
                        fireChangedEvent(e);
                    //}
                } else {
                    if (contains(e.getSource())) {
                        fireChangedEvent(e);
                    }
                }
            }
            //}

            @Override
            public boolean isDestroyed() {
                if (owner == null) {
                    return !self.unDestroyable;
                } else {
                    return owner.isDestroyed();
                }

            }
        };
        registerTableListener(tableListener,model);
    }
    
    protected void registerTableListener(ChangedListener tableListener,DataModel model) {
        model.addChangedListener(tableListener);
    }

    public abstract long getSize();

    public abstract SortedSet get(long startIndex, long elements);

    public abstract SortedSet get();

    public ListChangedEvent add(DataModel o) {
        ListChangedEvent e = new ListChangedEvent(this, o);
        if (model != null) {
            model.fireChangedEvent(e);
        }

        fireChangedEvent(e);
        return e;
    }

    public ListChangedEvent remove(DataModel o) {
        ListChangedEvent e = new ListChangedEvent(this, o);
        if (model != null) {
            model.fireChangedEvent(e);
        }
        fireChangedEvent(e);
        return e;
    }

    public abstract boolean contains(Object o);

    @Override
    public void destroy() {
        if (model != null) {
            model.removeChangedListener(tableListener);
        }
        super.destroy();
    }

    @Override
    public void resume() {
        if (model != null) {
            setParentModel(model);
        }
    }
    
    
    @Override
    public synchronized void cleanUp() {
        super.cleanUp(); 
         if(!hasListeners() || owner.isDestroyed()) {
            destroy();
        }
    }

}
