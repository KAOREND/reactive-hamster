package com.kaibla.hamster.persistence.events;

import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.DataModel;
import com.kaibla.hamster.persistence.model.ListModel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * @author Kai Orend
 */
public class ListChangedEvent extends DataEvent {

    private final DataModel databaseObject;
    private final LinkedList fireAgainLists = new LinkedList();

    /**
     * Creates a new instance of DatabaseObjectCreatedEvent
     */
    public ListChangedEvent(DataModel model, DataModel databaseObject) {
        super(model);
        this.databaseObject = databaseObject;
    }

    public void addFireAfterCommit(ListModel model) {
        fireAgainLists.add(model);
    }

    public void fireAfterCommit() {
        clearHistory();
        Iterator iter = fireAgainLists.iterator();
        while (iter.hasNext()) {
            ListModel model = (ListModel) iter.next();
            model.fireChangedEvent(this);
        }
    }

    public DataModel getDatabaseObject() {
        return databaseObject;
    }
    private static final Logger LOG = getLogger(ListChangedEvent.class.getName());

}
