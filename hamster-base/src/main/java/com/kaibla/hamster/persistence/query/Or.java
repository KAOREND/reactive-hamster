/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * .
 */
package com.kaibla.hamster.persistence.query;

import com.kaibla.hamster.persistence.model.Document;
import com.mongodb.BasicDBList;
import com.mongodb.client.model.Filters;
import java.util.ArrayList;
import java.util.List;
import org.bson.conversions.Bson;

/**
 *
 * @author kai
 */
public class Or extends Condition {

    List<Condition> conditions;

    public Or() {
    }

    public Or(List<Condition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean isInCondition(Document o) {
        for (Condition condition : conditions) {
            if (condition.isInCondition(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Bson buildQuery() {
        ArrayList<Bson> filters = new ArrayList(conditions.size());
        for (Condition c : conditions) {
            filters.add(c.buildQuery());
        }
        return Filters.or(filters);
    }
    
     @Override
    public Bson buildShadowQuery() {
         ArrayList<Bson> filters = new ArrayList(conditions.size());
        for (Condition c : conditions) {
            filters.add(c.buildShadowQuery());
        }
        return Filters.or(filters);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Or) {
            Or or = (Or) o;
            return or.conditions.equals(conditions);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (Condition cond : conditions) {
            hashCode = 31 * hashCode + cond.hashCode();
        }
        return hashCode;
    }

   
}
