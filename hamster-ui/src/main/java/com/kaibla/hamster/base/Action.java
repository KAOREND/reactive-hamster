/*
 * Action.java Created on 3. August 2007, 15:54
 */
package com.kaibla.hamster.base;

import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Kai Orend
 */
public abstract class Action implements Serializable {

    public abstract void invoke();
    private boolean leftEventFired = false;
    private String animation=null;

    public Action() {

    }

    public Action setAnimation(String animation) {
        this.animation = animation;
        return this;
    }

    public String getAnimation() {
        return animation;
    }
    
    

    public boolean hasUndo() {
        return false;
    }

    public void Redo() {
        invoke();
    }

    public void Undo() {

    }

    public String getStaticName() {
        return null;
    }

    public String getStaticParent() {
        return null;
    }

    public String getStaticParameter() {
        return null;
    }

    /**
     * @deprecated Use getStaticParent instead
     * @return
     */
    public String getStaticPrefix() {
        return null;
    }

    public void setStaticParameter(String s) {
    }

    public final String getStaticString() {
        String p = getStaticParameter();
        if (p != null) {
            return getStaticName() + "/" + p;
        } else {
            return getStaticName() + "//";
        }
    }

    public final void firePageLeftEvent() {
        if (!leftEventFired) {
            leftEventFired = true;
            pageLeftEvent();
        }
    }

    public final void fireMyPageLeftEvent() {
        myPageLeftEvent();
    }

    /**
     * Is invoked when the user leaves the current page. This works only with action with static links.
     */
    public void pageLeftEvent() {

    }

    public void myPageLeftEvent() {

    }

    public boolean isAllowed() {
        return true;
    }

    @Override
    public String toString() {
        return getClass().getName() + "  :" + getStaticString();
    }
}
