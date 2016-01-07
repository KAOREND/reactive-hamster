
package com.kaibla.hamster.components.form;

import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.util.Template;
import java.util.LinkedList;

/**
 *
 * @author kai
 */
public class NumberField extends InputField {

    public NumberField() {
        template=NUMBER_FIELD;
        type="NUMBER";
    }

    public NumberField(HamsterPage page, String name) {
        super(page, name);
         template=NUMBER_FIELD;
         type="number";
    }

    public NumberField(HamsterPage page, String name, String value) {
        super(page, name, value);
        template=NUMBER_FIELD;
        type="number";
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
            slots.add("onkeyup=\"hamster.main.submitWithTimeout('" + getParent().getId() + "','" + form.getFormActionURL() + "')\" onchange=\"hamster.main.submitWithTimeout('" + getParent().getId() + "','" + form.getFormActionURL() + "')\"");
        } else {
            slots.add("");
        }
        htmlCode = template.mergeStrings(slots, getPage());
    }
    
    
    private static transient Template NUMBER_FIELD = new Template(InputField.class.getResource("number_field.html"));
}
