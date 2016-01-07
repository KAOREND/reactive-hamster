/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * .
 */

package com.kaibla.hamster.components;

import com.kaibla.hamster.collections.StringSource;
import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.util.HTMLCodeFilter;

/**
 *
 * @author kai
 */
public class HTMLContentStringSource implements StringSource{
    StringSource wrappedStringSource;

    public HTMLContentStringSource(StringSource wrappedStringSource) {
        this.wrappedStringSource = wrappedStringSource;
    }    
    

    @Override
    public String toString() {
        return "<content>"+HamsterComponent.encode(wrappedStringSource.toString().trim())+"</content>";
    }  
    
}
