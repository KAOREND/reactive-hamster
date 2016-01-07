/*
 * ChangedListener.java Created on 18. Februar 2007, 18:30
 */
package com.kaibla.hamster.base;

import java.io.Serializable;

/**
 * @author Kai Orend
 */
public interface ChangedListener extends Serializable {

    public abstract void dataChanged(DataEvent e);

    public abstract boolean isDestroyed();
}
