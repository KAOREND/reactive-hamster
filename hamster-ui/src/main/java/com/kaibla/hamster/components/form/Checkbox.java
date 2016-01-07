package com.kaibla.hamster.components.form;

import com.kaibla.hamster.base.HamsterPage;
import static com.kaibla.hamster.util.HTMLCodeFilter.getStrictFilteredString;
import com.kaibla.hamster.util.Template;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class Checkbox extends FormElement {

    String name;
    private boolean checked = true;
    String css = "";
    private boolean showLabel = true;
    Template template = CHECKBOX;

    public Checkbox() {

    }

    public Checkbox(HamsterPage page, String name) {
        super(page);
        this.name = getStrictFilteredString(name);
    }

    public Checkbox(HamsterPage page, String name, boolean value) {
        super(page);
        this.name = getStrictFilteredString(name);

        this.checked = value;
    }

    public void setTemplate(Template t) {
        template = t;
    }

    @Override
    public String getName() {
        return name;
    }

    public Checkbox setCSSClass(String cl) {
        css = cl;
        return this;
    }

    @Override
    public Checkbox setValue(String value) {
        LOG.log(Level.INFO, "Checkbox: {0}", value);
        checked = value.equals(name) || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("checked") || value.equalsIgnoreCase("true");
        valueChanged();
        return this;
    }

    public void valueChanged() {

    }

    @Override
    public void generateHTMLCode() {
        LinkedList slots = new LinkedList();
        slots.add("" + getId());
        slots.add(name);
        slots.add(name);
        slots.add(css);
        if (checked) {
            slots.add("checked=\"checked\"");
        } else {
            slots.add("");
        }
        if (showLabel) {
            slots.add(name);
        } else {
            slots.add("");
        }
        htmlCode = template.mergeStrings(slots, getPage());
    }

    /**
     * @return the value
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * @param value the value to set
     */
    public void setChecked(boolean value) {
        this.checked = value;
        valueChanged();
    }

    private static transient Template CHECKBOX = new Template(Checkbox.class.getResource("checkbox.html"));

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

    @Override
    public String getStringValue() {
        return ""+isChecked();
    }
    
    
    private static final Logger LOG = getLogger(Checkbox.class.getName());
}
