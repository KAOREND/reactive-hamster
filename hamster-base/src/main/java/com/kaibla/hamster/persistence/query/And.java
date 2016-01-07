/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * .
 */
package com.kaibla.hamster.persistence.query;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.kaibla.hamster.persistence.model.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author kai
 */
public class And extends Condition {
    
    List<Condition> conditions;

    public And(List<Condition> conditions) {
        this.conditions = conditions;
    }
    
    public static And and(Condition ...conditions) {
        return new And(Arrays.asList(conditions));
    }

    @Override
    public boolean isInCondition(Document o) {
        for(Condition condition: conditions) {
            if(!condition.isInCondition(o)) return false;
        }
        return true;
    }

    public List<Condition> getConditions() {
        return conditions;
    }
    
    

    @Override
    public void buildQuery(DBObject parentQuery) {      
         BasicDBList s = new BasicDBList();
        for(Condition condition : conditions) {           
            condition.buildQuery(s);
        }       
        addToParentQuery(parentQuery, "$and", s);
    }
    
    @Override
    public boolean equals(Object o) {
        if(o instanceof And) {
           And and=(And) o;
           return and.conditions.equals(conditions);
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
