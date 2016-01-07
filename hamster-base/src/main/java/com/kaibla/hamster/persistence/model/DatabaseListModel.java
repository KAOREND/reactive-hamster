package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.base.ChangedListener;
import com.kaibla.hamster.base.DataModel;
import com.kaibla.hamster.base.AbstractListenerOwner;
import com.kaibla.hamster.persistence.model.ListModel;
import com.kaibla.hamster.persistence.query.Query;
import com.kaibla.hamster.persistence.query.BaseQuery;
import java.io.Serializable;

/**
 *
 * @author Kai Orend
 */
public abstract class DatabaseListModel extends ListModel implements Serializable {

    BaseQuery query;

    public DatabaseListModel(AbstractListenerOwner comp) {
        super(comp);

    }

    public DatabaseListModel(AbstractListenerOwner comp, DataModel model, BaseQuery query) {
        super(comp);
        this.query = query;
        setParentModel(model);
    }

    public BaseQuery getQuery() {
        return query;
    }

    @Override
    protected void registerTableListener(ChangedListener tableListener, DataModel model) {
        DocumentCollection table = (DocumentCollection) model;
        table.addChangedListener(tableListener, (Query) query);
    }

    @Override
    public void removeChangedListener(ChangedListener listener) {
        super.removeChangedListener(listener);
//        if(!hasListeners()) {
//            destroy();
//        }
    }

    public abstract long getSize(int max);

}
