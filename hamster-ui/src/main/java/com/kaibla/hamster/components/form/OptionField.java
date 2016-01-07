package com.kaibla.hamster.components.form;

import com.kaibla.hamster.base.HamsterPage;
import static com.kaibla.hamster.util.HTMLCodeFilter.getStrictFilteredString;
import com.kaibla.hamster.util.Template;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * This component can be used for combo boxes and radio boxes.
 *
 * @author Kai Orend
 */
public class OptionField extends FormElement {

    String name;
    String css = "";
    Template template;
    Template radioTemplate;
    LinkedList options = new LinkedList();
    HashMap optionMap = new HashMap();
    boolean radio = false;
    Option selected;
    private boolean showLabel = true;
    OptionField self = this;
    private boolean submitOnSelect = false;
    private Form form;
    private boolean translateLabels = true;

    /**
     *
     */
    public OptionField() {
    }

    /**
     *
     * @param page
     * @param name Name of the Option Field
     * @param radio If true alle options will be shown as radio boxes otherwise this component will be displayed as
     * combo box.
     */
    public OptionField(HamsterPage page, String name, boolean radio, boolean translateLabels) {
        super(page);
        this.name = getStrictFilteredString(name);
        this.radio = radio;
        if (radio) {
            template = RADIO;
        } else {
            template = COMBO;
        }
        this.translateLabels = translateLabels;
    }

    public OptionField(HamsterPage page, String name, boolean radio) {
        super(page);
        this.name = getStrictFilteredString(name);
        this.radio = radio;
        if (radio) {
            template = RADIO;
        } else {
            template = COMBO;
        }
    }

    public void setSubmitOnSelect(boolean submitOnSelect, Form form) {
        this.submitOnSelect = submitOnSelect;
        this.form = form;
    }

    public boolean isSubmitOnSelect() {
        return submitOnSelect;
    }

    /**
     *
     * @return The name of the currently selected option
     */
    public String getSelected() {
        return selected.oname;
    }

    /**
     *
     * @return The object of the currently selected option
     */
    public Object getSelectedObject() {
        return selected.userObject;
    }

    /**
     * Adds a new option.
     *
     * @param name The name of the Option
     * @param selected The default value for the Option
     * @param handler The handler is called when the value of the option is changed by the user.
     */
    public void addOption(String name, boolean selected, OnSelectionChangeHandler handler) {
        addOption(null, name, selected, handler);
    }

    /**
     * Adds a new option.
     *
     * @param name The name of the Option
     *
     * @param selected The default value for the Option
     */
    public OptionField addOption(String name, boolean selected) {
        addOption(null, name, selected, null);
        return this;
    }

    /**
     * Adds a new option.
     *
     * @param name The name of the Option
     * @param selected The default value for the Option
     * @param handler The handler is called when the value of the option is changed by the user.
     */
    public OptionField addOption(Object userObject, String name, boolean selected, OnSelectionChangeHandler handler) {

        Option opt = new Option();
        opt.oname = name;
        if (translateLabels) {
            opt.name = getStrictFilteredString(getTranslatedString(name));
        } else {
            opt.name = getStrictFilteredString(name);
        }
        opt.handler = handler;
        opt.userObject = userObject;
        opt.selected = selected;
        options.add(opt);
        optionMap.put(opt.oname, opt);
        if (selected || this.selected == null) {
            this.selected = opt;
            opt.selected = true;
            if (handler != null) {
                handler.selectionChanged(opt.oname, selected, this);
            }
        }
        markForUpdate();
        return this;
    }

    /**
     *
     */
    @Override
    public void generateHTMLCode() {

        if (radio) {
            String s = "<div id=\"" + getId() + "\">";
            Iterator iter = options.iterator();
            while (iter.hasNext()) {
                Option o = (Option) iter.next();
                LinkedList slots = new LinkedList();
                slots.add(getName());
                slots.add(o.name);
                slots.add(css);
                if (o.selected) {
                    slots.add("checked=\"checked\"");
                } else {
                    slots.add("");
                }
                if (isShowLabel()) {
                    slots.add(o.name);
                } else {
                    slots.add("");
                }
                s += template.mergeStrings(slots, getPage());
            }
            s += "</div>";
            htmlCode = s;
        } else {
            LinkedList slots = new LinkedList();
            slots.add("" + getId());
            slots.add(name);
            slots.add(css);
            if (submitOnSelect) {
                slots.add("onchange=\"hamster.main.autoSubmitForm('" + form.getId() + "','" + form.getFormActionURL() + "')\"");
            } else {
                slots.add("");
            }
            String s = "";
            Iterator iter = options.iterator();
            while (iter.hasNext()) {
                Option o = (Option) iter.next();
                if (o.selected) {
                    s += "<option selected=\"true\" value=\"" + o.oname + "\" >" + o.name + "</option>";
                } else {
                    s += "<option value=\"" + o.oname + "\">" + o.name + "</option>";
                }
            }
            slots.add(s);
            htmlCode = template.mergeStrings(slots, getPage());
        }

    }

    /**
     * Removes an option.
     *
     * @param name The name of the Option.
     */
    public void removeOption(String name) {
        Iterator iter = options.iterator();
        while (iter.hasNext()) {
            Option o = (Option) iter.next();
            if (o.name.equals(name)) {
                iter.remove();
            }
        }
        optionMap.remove(name);
        markForUpdate();
    }

    /*Sets the Template for this Option field. */
    /**
     *
     * @param t The Template.
     */
    public void setTemplate(Template t) {
        template = t;
    }

    /**
     *
     * @return The name of this form element.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     *
     * @param cl The CSS class for this element.
     * @return this.
     */
    public OptionField setCSSClass(String cl) {
        css = cl;
        return this;
    }

    @Override
    public OptionField setValue(String value) {
        LOG.log(Level.INFO, "Option Field Value: {0}", value);
        Option opt = (Option) optionMap.get(value);
        if (opt == null) {
            LOG.log(Level.INFO, "opt not found: {0}", value);
        } else if (opt != selected) {
            if (selected.handler != null) {
                selected.handler.selectionChanged(name, false, this);
            }
            selected.selected = false;
            opt.selected = true;
            if (opt.handler != null) {
                opt.handler.selectionChanged(opt.name, true, this);
            }
            selected = opt;
        }
        return this;
    }

    /**
     * @return the showLabel
     */
    public boolean isShowLabel() {
        return showLabel;
    }

    /**
     * @param showLabel the showLabel to set
     */
    public void setShowLabel(boolean showLabel) {
        this.showLabel = showLabel;
    }

    /**
     * The Handler for selection changed events.
     */
    public interface OnSelectionChangeHandler {

        /**
         * Is called, when the value of the option has changed.
         *
         * @param name The name of the option.
         * @param selected The new value of the option.
         */
        abstract public void selectionChanged(String name, boolean selected, OptionField opt);
    }

    private class Option implements Serializable {

        String name, oname;
        Object userObject;
        OnSelectionChangeHandler handler;
        boolean selected;
    }

    @Override
    public String getStringValue() {
        return getSelected();
    }
    
    private static transient Template COMBO = new Template(OptionField.class.getResource("combobox.html"));
    private static transient Template RADIO = new Template(OptionField.class.getResource("radio.html"));
    private static final Logger LOG = getLogger(OptionField.class.getName());
}
