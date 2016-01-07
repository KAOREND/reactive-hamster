package com.kaibla.hamster.components;

import java.io.Serializable;

/**
 * This class is used for callback functions.
 *
 * @author kai
 */
public abstract class Function implements Serializable {

    /**
     * This method contains the user code of the callback function.
     */
    abstract public void invoke();

    /**
     *
     * @return True if the Function was already activated and does not need to be called again.
     */
    public boolean isActive() {
        return false;
    }
}
