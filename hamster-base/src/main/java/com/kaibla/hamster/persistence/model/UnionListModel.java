package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.model.DatabaseListModel;
import com.kaibla.hamster.base.AbstractListenerOwner;
import com.kaibla.hamster.base.HamsterEngine;
import com.kaibla.hamster.persistence.query.BaseQuery;
import java.util.TreeSet;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author kai
 */
public class UnionListModel extends DatabaseListModel {

    DatabaseListModel list1;
    DatabaseListModel list2;
    BaseQuery orderQuery;
    TreeSet fetched;
    long fetchedIndex;

    public UnionListModel(DatabaseListModel list1, DatabaseListModel list2, BaseQuery orderQuery, AbstractListenerOwner owner) {
        super(owner);
        this.list1 = list1;
        this.list2 = list2;
        this.orderQuery = orderQuery;
        fetched = new TreeSet(orderQuery);
    }

    @Override
    public long getSize(int max) {
        return list1.getSize(max) + list2.getSize(max);
    }

    @Override
    public long getSize() {
        return list1.getSize() + list2.getSize();
    }

    @Override
    public TreeSet get(long startIndex, long elements) {
//       long wantedElements =startIndex+elements;
        return fetched;
    }

    @Override
    public TreeSet get() {
        TreeSet all = new TreeSet(orderQuery);
        all.addAll(list1.get());
        all.addAll(list2.get());
        return all;
    }

    @Override
    public boolean contains(Object o) {
        return list1.contains(o) || list2.contains(o);
    }

    public Object prepareResume(HamsterEngine engine) {
        return this;
    }
    private static final Logger LOG = getLogger(UnionListModel.class.getName());

}
