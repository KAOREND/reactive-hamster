package com.kaibla.hamster.persistence.query;

import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.conversions.Bson;

/**
 *
 * @author Kai Orend
 */
public class Query implements BaseQuery {

    List<Condition> conditions = new LinkedList<>();
    List<SortCriteria> sortCriterias = new LinkedList<>();
    List<Equals> eventFilterConditions = new ArrayList<>();

    public Query() {
      
    }

    public List<Equals> getEventFilter() {
        return eventFilterConditions;
    }

    public Query addEventFilter(Equals attr) {
        eventFilterConditions.add(attr);
        return this;
    }

    public Query equals(Attribute attr, Object value) {
        conditions.add(new Equals(attr, value));
        return this;
    }

    public Query notEquals(Attribute attr, Object value) {
        conditions.add(new NotEquals(attr, value));
        return this;
    }

//    public Query equals(LongTextAttribute attr, Object value) {
//        org.bson.Document s = new org.bson.Document();
//        s.put("$search", value);
////        org.bson.Document c = new org.bson.Document();
////        c.put("$text", s);
//        query.put("$text", value);
//        conditions.add(new Equals(attr, value));
//        return this;
//    }
    public Query greater(Attribute attr, Object value) {
        conditions.add(new Greater(attr, value));
        return this;
    }

    public Query greaterOrEquals(Attribute attr, Object value) {
        conditions.add(new GreaterOrEquals(attr, value));
        return this;
    }

    public Query lower(Attribute attr, Object value) {
        conditions.add(new Lower(attr, value));
        return this;
    }

    public Query lowerOrEquals(Attribute attr, Object value) {
        conditions.add(new LowerOrEquals(attr, value));
        return this;
    }

    public Query addSortCriteria(Attribute attr, boolean descending) {
        sortCriterias.add(new SortCriteria(attr, descending));
        return this;
    }


    public List<Condition> getConditions() {
        return conditions;
    }

    public Query addCondition(Condition condition) {
        conditions.add(condition);
        if (condition instanceof And) {
            And and = (And) condition;
            addEventFilter(and);
        }
        return this;
    }

    private void addEventFilter(And and) {
        for (Condition c : and.getConditions()) {
            if (c instanceof Equals) {
                addEventFilter((Equals) c);
            } else if (c instanceof And) {
                addEventFilter((And) c);
            }
        }
    }

    @Override
    public Bson getQuery() {
        Bson query = new BsonDocument();
        if (conditions.size() > 1) {
            And and = new And(conditions);
            query = and.buildQuery();
        } else if (conditions.size() == 1) {
            query = conditions.get(0).buildQuery();
        }
        return query;
    }

    @Override
    public Bson getSort() {
        BsonDocument orderBy = new BsonDocument();
        for (SortCriteria sortCriteria : sortCriterias) {
            if (sortCriteria.isDescending()) {
                orderBy.put(sortCriteria.getAttribute().getName(), new BsonInt32(-1));
            } else {
                orderBy.put(sortCriteria.getAttribute().getName(), new BsonInt32(1));
            }
        }
        return orderBy;
    }

    @Override
    public boolean isInQuery(Document o) {
        for (Condition cond : conditions) {
            if (!cond.isInCondition(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int compare(Document o1, Document o2) {
        for (SortCriteria order : sortCriterias) {
            int diff = order.compare(o1, o2);
            if (diff != 0) {
                return diff;
            }
        }
        if (o1.getId().equals(o2.getId()) && o1 != o2) {
            throw new IllegalStateException("compare failed, ids cannot be equal for different mongo objects");
        }
        return String.CASE_INSENSITIVE_ORDER.compare(o1.getId(), o2.getId());
    }

    @Override
    public int compare(Object t, Object t1) {
        return compare((Document) t, (Document) t1);
    }

    @Override
    public boolean isOrderAttribute(Attribute attr) {
        for (SortCriteria order : sortCriterias) {
            if (order.getAttribute() == attr) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Query) {
            Query q = (Query) o;
            return conditions.equals(q.conditions) && sortCriterias.equals(q.sortCriterias);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (Condition cond : conditions) {
            hashCode = 31 * hashCode + cond.hashCode();
        }
        for (SortCriteria order : sortCriterias) {
            hashCode = 31 * hashCode + order.hashCode();
        }
        return hashCode;
    }

    private static final Logger LOG = getLogger(Query.class
            .getName());
}
