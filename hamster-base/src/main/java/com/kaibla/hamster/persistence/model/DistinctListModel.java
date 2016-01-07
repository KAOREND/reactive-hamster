package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.base.ChangedListener;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.query.BaseQuery;
import com.kaibla.hamster.collections.BalancedTree;
import java.util.Collection;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author kai
 */
public class DistinctListModel extends DatabaseListModel {

    DatabaseListModel sourceModel;
    Attribute distinctAttribute;
    transient BalancedTree<Document> resultCache;
    transient HashSet<Object> distinctSet;
    long lookAhead = 100;
    transient long lastRequestIndex = 0;

    public DistinctListModel(DatabaseListModel model, Attribute distinctAttribute) {
        super(model.getOwner());
        sourceModel = model;
        this.distinctAttribute = distinctAttribute;
        resultCache = new BalancedTree(model.getQuery());
        distinctSet = new HashSet();
//        sourceModel.addChangedListener(this);        
    }

    @Override
    public long getSize(int max) {
        return get(0, max).size();
    }

    @Override
    public long getSize() {
        return get().size();
    }

    @Override
    public SortedSet get(long startIndex, long elements) {
        long targetIndex = startIndex + elements;
        while (resultCache.size() < targetIndex) {
            SortedSet<Document> set = sourceModel.get(lastRequestIndex, lookAhead);
            if (set.isEmpty()) {
                return resultCache;
            } else {
                addToCache(set);
                lastRequestIndex += lookAhead;
            }
        }
        return resultCache;
    }

    private void addToCache(Collection<Document> set) {
        for (Document m : set) {
            Object distinctValue = m.get(distinctAttribute);
            if (!distinctSet.contains(distinctValue)) {
                distinctSet.add(distinctValue);
                resultCache.add(m);
            }
        }
    }

    @Override
    public SortedSet get() {
        addToCache(sourceModel.get());
        return resultCache;
    }

    @Override
    public boolean contains(Object o) {
        return sourceModel.contains(o);
    }

//    public void dataChanged(DataEvent e) {      
//        this.fireChangedEvent(e);
//    }
    public Object prepareResume(HamsterEngine engine) {
        return this;
    }

    @Override
    public void addChangedListener(ChangedListener listener) {
        sourceModel.addChangedListener(listener);
    }

    @Override
    public void removeChangedListener(ChangedListener listener) {
        sourceModel.removeChangedListener(listener); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BaseQuery getQuery() {
        return sourceModel.getQuery(); //To change body of generated methods, choose Tools | Templates.
    }
    private static final Logger LOG = getLogger(DistinctListModel.class.getName());
}
