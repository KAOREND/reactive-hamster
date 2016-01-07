/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * .
 */
package com.kaibla.hamster.persistence.model;

import com.kaibla.hamster.persistence.model.FilteredModel;
import com.kaibla.hamster.persistence.attribute.Attribute;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author kai
 */
public class AttributeFilter implements Serializable {
    private static final long serialVersionUID = 1L;

    FilteredModel model;
    List<Attribute> criterias;
    
    public AttributeFilter(FilteredModel model, List<Attribute> attributes) {
        this.model = model;
        this.criterias = attributes;
    }

    @Override
    public int hashCode() {
        int hashCode=1;
        for(Attribute attr : criterias) {
             hashCode = 31 * hashCode +attr.hashCode();
        }
        hashCode = 31 * hashCode +model.hashCode();
        return hashCode; 
    }

    @Override
    public boolean equals(Object o) {
        AttributeFilter a=(AttributeFilter) o;
        return model == a.model && criterias.equals(a.criterias);
    }

    
    
    public void setCriteria(List<Attribute> attributes) {
        this.criterias = attributes;
    }

    public void setModel(FilteredModel model) {
        this.model = model;
    }

    public FilteredModel getModel() {
        return model;
    }

    public List<Attribute> getCriteria() {
        return criterias;
    }
    
    

}
