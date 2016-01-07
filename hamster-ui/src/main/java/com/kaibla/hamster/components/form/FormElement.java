package com.kaibla.hamster.components.form;

import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterPage;

/**
 *
 * @author kai
 */
public abstract class FormElement extends HamsterComponent {

    public abstract FormElement setValue(String value);

    public abstract String getName();

    private boolean focusRequested = false;
    private boolean continousFocus = false;
    private boolean focused = false;

    private String formId = null;
    /**
     * Form Elements with higher Priority get the requestFocus
     */
    private int focusPriority = 0;

    public FormElement() {
    }

    public FormElement(HamsterPage page) {
        super(page);
        page.registerOnShowListener(this);
    }
    
    public abstract String getStringValue();

    @Override
    public String getId() {
        if (formId == null) {
            //check if we can use the name
            String name = getName();
            if (name != null) {
                HamsterComponent h = getPage().getComponentMap().get(name);
                if (h == null || h == this) {
                    formId = name;
                    getPage().getComponentMap().put(name, this);
                    return "" + formId;
                }
            }
            return "" + super.getId();
        }
        return "" + formId;
    }

    @Override
    public void onShow() {
        if (focusRequested && !page.isMobile()) {
            requestFocus();
            if (getPage().getFocusedElement() == this && (continousFocus || !focused) && !page.isMobile()) {
                forceFocus();
                focused = true;
            }
        }
    }

    public FormElement requestContinousFocus(int priority) {
        continousFocus = true;
        requestFocus(priority);
        return this;
    }

    public FormElement setContinousFocus(boolean active) {
        continousFocus = active;
        return this;
    }

    public FormElement requestFocus(int priority) {
        focusPriority = priority;
        requestFocus();
        return this;
    }

    public FormElement forceFocus() {
        exec("hamster.main.setFocus('" + getId() + "')");
        return this;
    }

    public FormElement requestFocus() {
        focusRequested = true;
        FormElement currentFocus = getPage().getFocusedElement();
        if (currentFocus == null || !currentFocus.isVisible() || currentFocus.focusPriority <= focusPriority) {
            getPage().setFocusedElement(this);
            if (currentFocus != null && currentFocus != this && !page.isMobile()) {
                String focusString = "hamster.main.setFocus('" + currentFocus.getId() + "')";
                getPage().getModificationManager().removeScript(focusString);
                currentFocus.focused = false;
            }
        }
        return this;
    }

    
    
    

}
