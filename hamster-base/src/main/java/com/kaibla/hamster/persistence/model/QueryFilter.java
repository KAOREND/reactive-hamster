/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * .
 */
package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.query.Query;
import com.kaibla.hamster.persistence.model.DocumentCollection;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author kai
 */
public class QueryFilter implements Serializable {
    private static final long serialVersionUID = 1L;

   private DocumentCollection model;
   private Query query;

    public QueryFilter(DocumentCollection model, Query query) {
        this.model = model;
        this.query = query;
    }

    public DocumentCollection getModel() {
        return model;
    }

    public Query getQuery() {
        return query;
    }
    
    @Override
    public int hashCode() {
        int hashCode=1;
       
        hashCode = 31 * hashCode +model.hashCode();
        hashCode = 31 * hashCode +query.hashCode();
        return hashCode; 
    }

    @Override
    public boolean equals(Object o) {
        QueryFilter a=(QueryFilter) o;
        return model == a.model && query.equals(a.query);
    }
}
