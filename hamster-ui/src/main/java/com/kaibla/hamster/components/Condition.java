package com.kaibla.hamster.components;

import java.io.Serializable;

/**
 *
 * @author Kai Orend
 */
public interface Condition extends Serializable {

    /**
     *
     * @return true if the action is allowed
     */
    public abstract boolean isAllowed();
}
