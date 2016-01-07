package com.kaibla.hamster.persistence.query;

import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class Query implements BaseQuery {

    BasicDBObject wholeQuery;
    BasicDBObject query;
    BasicDBObject orderBy;
    List<Condition> conditions = new LinkedList<Condition>();
    List<Order> orders = new LinkedList<Order>();
    List<Equals> eventFilterConditions=new ArrayList<Equals>();
    
    public Query() {
        wholeQuery = new BasicDBObject();
        query = new BasicDBObject();
        orderBy = new BasicDBObject();
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
//        BasicDBObject s = new BasicDBObject();
//        s.put("$search", value);
////        BasicDBObject c = new BasicDBObject();
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
         
    public Query addOrder(Attribute attr, boolean descending) {
        if (descending) {
            orderBy.put(attr.getName(), -1);
        } else {
            orderBy.put(attr.getName(), 1);
        }
        orders.add(new Order(attr, descending));
        return this;
    }

    private void buildQuery() {
        query = new BasicDBObject();
        wholeQuery = new BasicDBObject();
        for (Condition condition : conditions) {
            condition.buildQuery(query);
        }
        wholeQuery.put("$query", query);
        wholeQuery.put("$orderby", orderBy);
    }

    public List<Condition> getConditions() {
        return conditions;
    }
    
    public void addCondition(Condition condition) {
        conditions.add(condition);
        if(condition instanceof And) {
            And and =(And) condition;
            addEventFilter(and);
        }
    }
    
    private void addEventFilter(And and) {
        for(Condition c : and.getConditions()) {
                if(c instanceof Equals) {
                    addEventFilter((Equals) c);
                } else if(c instanceof And) {
                    addEventFilter((And) c);
                }
            } 
    }

    @Override
    public BasicDBObject getQuery() {
        buildQuery();
        LOG.fine("building query: "+JSON.serialize(wholeQuery));        
        return wholeQuery;
    }

    @Override
    public BasicDBObject getQueryPartOnly() {
        buildQuery();
        return query;
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
        for (Order order : orders) {
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
        for (Order order : orders) {
            if (order.getAttribute() == attr) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Query) {
            Query q=(Query) o;
            return conditions.equals(q.conditions) && orders.equals(q.orders);
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        int hashCode=1;
        for(Condition cond : conditions) {
             hashCode = 31 * hashCode +cond.hashCode();
        }  
        for(Order order : orders) {
             hashCode = 31 * hashCode +order.hashCode();
        } 
        return hashCode; 
    }
    
    
    private static final Logger LOG = getLogger(Query.class
            .getName());
}
