package com.kaibla.hamster.components.form;

import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.collections.StringSource;
import static com.kaibla.hamster.util.HTMLCodeFilter.getStrictFilteredString;
import com.kaibla.hamster.util.Template;
import java.util.LinkedList;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class InputField extends FormElement {

    String name;
    String value = "";
    String css = "";
    int size = 40;
    boolean password = false;
    boolean submitOnKeyUp = false;
    Template template = INPUT;
    StringSource placeholder;
    Form form;
    String type;
    public InputField() {
    }

    public InputField(HamsterPage page, String name) {
        super(page);
        page.registerOnShowListener(this);
        this.name = getStrictFilteredString(name);
    }

    public InputField(HamsterPage page, String name, String value) {
        super(page);
        if (value == null) {
            value = "";
        }
        this.name = getStrictFilteredString(name);
        ;
        this.value = value;
    }

    public void setType(String type) {
        this.type = type;
    }

    
    public void setForm(Form form) {
        this.form = form;
    }

    public void setTemplate(Template t) {
        template = t;
    }

    public String getValue() {
        return getStrictFilteredString(value);
    }

    public InputField setSubmitOnKeyUp(boolean submitOnKeyUp) {
        this.submitOnKeyUp = submitOnKeyUp;
        return this;
    }

    public String getUnfilteredtValue() {
        return value;
    }

    public int getSize() {
        return size;
    }

    public InputField setSize(int size) {
        this.size = size;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public InputField setCSSClass(String cl) {
        css = cl;
        return this;
    }

    @Override
    public InputField setValue(String value) {
        this.value = value;
        valueChanged();
        return this;
    }

    public void valueChanged() {
    }

    @Override
    public void onShow() {
        super.onShow();
//        if (submitOnKeyUp) {
//            requestFocus();
//        }
    }

    @Override
    public void generateHTMLCode() {
        LinkedList slots = new LinkedList();
        slots.add("" + getId());
        if (password) {
            slots.add("password");
        } else if(type != null) {
            slots.add(type);
        } else {
             slots.add("text");
        }
        slots.add(name);
        slots.add(value);
        slots.add(css);
        if(placeholder != null)  {
            slots.add(placeholder);
        } else {
            slots.add("");
        }
        slots.add("" + size);
        if (submitOnKeyUp) {
            slots.add("onkeyup=\"hamster.main.submitWithTimeout('" + getParent().getId() + "','" + form.getFormActionURL() + "')\"");
        } else {
            slots.add("");
        }
        htmlCode = template.mergeStrings(slots, getPage());
    }
    private static transient Template INPUT = new Template(InputField.class.getResource("input.html"));

    /**
     * @return the password
     */
    public boolean isPassword() {
        return password;
    }

    public InputField setPlaceholder(StringSource placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    /**
     * @param password the password to set
     */
    public InputField setPassword(boolean password) {
        this.password = password;
        return this;
    }
    private static final Logger LOG = getLogger(InputField.class.getName());

    @Override
    public String getStringValue() {
        return getValue();
    }
}
