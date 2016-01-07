/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * .
 */
package com.kaibla.hamster.persistence.query;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.kaibla.hamster.persistence.attribute.Attribute;
import com.kaibla.hamster.persistence.model.Document;
import java.util.LinkedList;
import java.util.List;

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
        for(Condition condition: conditions) {
            if(condition.isInCondition(o)) return true;
        }
        return false;
    }

    @Override
    public void buildQuery(DBObject parentQuery) {
        BasicDBList ors=new BasicDBList();
        for(Condition condition : conditions) {
            BasicDBObject clause=new BasicDBObject();
            condition.buildQuery(clause);
            ors.add(clause);
        }
        addToParentQuery(parentQuery, "$or", ors);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Or) {
           Or or=(Or) o;
           return or.conditions.equals(conditions);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        int hashCode=1;
        for(Condition cond : conditions) {
             hashCode = 31 * hashCode +cond.hashCode();
        }      
        return hashCode; 
    }
}
