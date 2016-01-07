/*
 * HamsterAnimation.java Created on 23. März 2007, 22:41
 * edited on 03. Oktober 2009
 */
package com.kaibla.hamster.base;

import java.util.HashMap;

/**
 * @author Kai Orend Stefan Birnkammerer
 */
public abstract class HamsterAnimation {

    private final HashMap<String, String> paramMap;

    /**
     * Creates a new instance of HamsterAnimation
     */
    public HamsterAnimation() {
        this.paramMap = new HashMap<String, String>();
    }

    /* All additional parameters for a specific animation subclass of HamsterAnimation
     * must be put into the paramMap by invoking updateParams() with the parameter pair*/
    public void updateParams(String key, String value) {
        this.paramMap.put(key, value);
    }
    /* Every subclass of HamsterAnimation must overwrite this method
     * and specify a JS function that will be invoked when a component with
     * the animation is replaced
     */

    public abstract String getJSFunction();

    /**
     * Generates a XML representation of the animation.
     */
    public String getXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<animation");
        buf.append(" jsFunction=\"").append(this.getJSFunction()).append("\"");
        buf.append(this.getAnimationParams());
        buf.append(">").append("</animation>");
        return buf.toString();
    }

    ;

    public String getAnimationParams() {
        StringBuilder buf = new StringBuilder();
        for (String key : this.paramMap.keySet()) {
            String value = this.paramMap.get(key);
            buf.append(" ").append(key).append("=\"").append(value).append("\"");
        }
        return buf.toString();
    }

    ;

    @Override
    public String toString() {
        return getXML();
    }
}
