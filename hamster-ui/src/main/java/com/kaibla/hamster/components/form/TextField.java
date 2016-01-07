package com.kaibla.hamster.components.form;

import com.kaibla.hamster.base.HamsterPage;
import static com.kaibla.hamster.util.HTMLCodeFilter.getStrictFilteredString;
import com.kaibla.hamster.util.Template;
import java.util.LinkedList;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class TextField extends FormElement {

    String name;
    String value = "";
    String css = "";
    private int cols = 20;
    private int rows = 4;
    Template template = TEXT;
    private boolean submitOnKeyUp = false;
    Form form;
    
    public TextField() {
    }

    public TextField(HamsterPage page, String name) {
        super(page);
        this.name = getStrictFilteredString(name);
    }

    public TextField(HamsterPage page, String name, String value) {
        super(page);
        this.name = getStrictFilteredString(name);
        if (value == null) {
            this.value = "";
        } else {
            this.value = value;
        }
    }

    public void setSubmitOnKeyUp(boolean submitOnKeyUp,Form form) {
        this.submitOnKeyUp = submitOnKeyUp;
        this.form=form;
    }
    
    

    public void setTemplate(Template t) {
        template = t;
    }

    public String getValue() {
        return getStrictFilteredString(value);
    }

    public String getUnfilteredtValue() {
        return value;
    }

    @Override
    public String getName() {
        return name;
    }

    public TextField setCSSClass(String cl) {
        css = cl;
        return this;
    }

    @Override
    public TextField setValue(String value) {
        this.value = value;
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
        slots.add(css);
        slots.add("" + getRows());
        slots.add("" + getCols());
        if (submitOnKeyUp) {
            slots.add("onkeyup=\"hamster.main.submitWithTimeout('" + getParent().getId() + "','" + form.getFormActionURL() + "')\"");
        } else {
            slots.add("");
        }
        slots.add(value);
        htmlCode = template.mergeStrings(slots, getPage());
    }
    private static transient Template TEXT = new Template(TextField.class.getResource("textfield.html"));

    /**
     * @return the cols
     */
    public int getCols() {
        return cols;
    }

    /**
     * @param cols the cols to set
     */
    public TextField setCols(int cols) {
        this.cols = cols;
        return this;
    }

    /**
     * @return the rows
     */
    public int getRows() {
        return rows;
    }
    

    /**
     * @param rows the rows to set
     */
    public TextField setRows(int rows) {
        this.rows = rows;
        return this;
    }
    
     @Override
    public String getStringValue() {
        return getValue();
    }
    
    private static final Logger LOG = getLogger(TextField.class.getName());
}
