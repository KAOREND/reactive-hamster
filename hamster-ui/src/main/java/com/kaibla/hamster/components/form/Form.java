package com.kaibla.hamster.components.form;

import com.kaibla.hamster.base.Action;
import com.kaibla.hamster.base.UIContext;
import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.components.defaultcomponent.DefaultComponent;
import static com.kaibla.hamster.util.HTMLCodeFilter.getStrictFilteredString;
import com.kaibla.hamster.util.Template;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public abstract class Form extends DefaultComponent {
    private static final long serialVersionUID = 1L;

    private final LinkedList<FormElement> formElements = new LinkedList();
    private final HashMap<String, FormElement> formMap = new HashMap();
    private String cssClass = "";
    private boolean resetAfterSending = false;
    private boolean wasAutoSubmitted = false;
    private String onSubmit = "";

    public Form() {
    }

    public Form(HamsterPage page) {
        super(page);
        template = null;
    }

    /**
     *
     * @param name Name of the Form Element
     * @return The String value
     */
    public String getString(String name) {
        FormElement fe = (FormElement) formMap.get(name);
        return fe.getStringValue();
    }

    public FormElement getFormElement(String name) {
        return (FormElement) formMap.get(name);
    }

    public boolean wasAutoSubmitted() {
        return wasAutoSubmitted;
    }

    /**
     *
     * @param name Name of the Form Element
     * @return The Object value
     */
    public Object getObject(String name) {
        FormElement fe = (FormElement) formMap.get(name);
        if (fe instanceof OptionField) {
            return ((OptionField) fe).getSelectedObject();
        }
        return null;
    }

    /**
     *
     * @param name Name of the Form Element
     * @return The boolean value
     */
    public boolean getBoolean(String name) {
        FormElement fe = (FormElement) formMap.get(name);
        if (fe instanceof Checkbox) {
            return ((Checkbox) fe).isChecked();
        }
        return false;
    }

    public Form addElement(FormElement comp) {
        formElements.add(comp);
        elements.add(comp);
        addComponent(comp);
        formMap.put(comp.getName(), comp);
        if (comp instanceof InputField) {
            InputField in = (InputField) comp;
            in.setForm(this);
        }
        return this;
    }

    public Form addAsFormElementOnly(FormElement comp) {
        formElements.add(comp);
        formMap.put(comp.getName(), comp);
        if (comp instanceof InputField) {
            InputField in = (InputField) comp;
            in.setForm(this);
        }
        return this;
    }

    public void removeFormElement(FormElement comp) {
        formElements.remove(comp);
        formMap.remove(comp.getName());
    }

    public void removeAllFormElements() {
        for (FormElement elem : new LinkedList<FormElement>(formElements)) {
            removeFormElement(elem);
        }
    }

    public void addSubmitButton(String text) {
        text = getStrictFilteredString(text);
        elements.add("<input type=\"submit\" value=\"" + text + "\"  name=\"" + text + "\"/>");
    }

    public void addResetButton(String text) {
        text = getStrictFilteredString(text);
        elements.add("<input type=\"reset\" value=\"" + text + "\"  name=\"" + text + "\"/>");
    }

    public Action createSubmitAction() {
        return new SubmitAction();
    }

    public String getFormActionURL() {
        return getSubmitActionURL(new SubmitAction());
    }

    @Override
    public void generateHTMLCode() {
        Template currentTemplate = this.template;
        if (currentTemplate == null) {
            currentTemplate = FORM;
        }
        LinkedList slots = new LinkedList();
        String s = "";

        slots.add("" + getId());
        slots.add("" + getId());

        slots.add(getSubmitAction(new SubmitAction(), isResetAfterSending()));

        if (this.template == null) {
            slots.add(getCssClass());
        }
        slots.add(onSubmit);
        if (isOneSlot()) {
            Iterator i = elements.iterator();
            i.next();
            while (i.hasNext()) {
                Object o = i.next();
                if (o instanceof Action) {
                    s += getActionLinkTag((Action) o);
                } else {
                    s += o;
                }
            }
            slots.add(s);
        } else {
            Iterator i = elements.iterator();
            i.next();
            while (i.hasNext()) {
                Object o = i.next();
                if (o instanceof Action) {
                    slots.add(getActionLinkTag((Action) o));
                } else {
                    slots.add(o);
                }
            }
        }
        htmlCode = currentTemplate.mergeStrings(slots, getPage());
    }

    /**
     * @return the cssClass
     */
    public String getCssClass() {
        return cssClass;
    }

    /**
     * @param cssClass the cssClass to set
     */
    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    /**
     * @return the resertAfterSending
     */
    public boolean isResetAfterSending() {
        return resetAfterSending;
    }

    /**
     * @param resertAfterSending the resertAfterSending to set
     */
    public Form setResetAfterSending(boolean resertAfterSending) {
        this.resetAfterSending = resertAfterSending;
        return this;
    }

    /**
     * @return the onSubmit
     */
    public String getOnSubmit() {
        return onSubmit;
    }

    /**
     * @param onSubmit the onSubmit to set
     */
    public void setOnSubmit(String onSubmit) {
        this.onSubmit = onSubmit;
    }

    public class SubmitAction extends Action {

        @Override
        public void invoke() {
            wasAutoSubmitted = false;
            Map map = UIContext.getParameterMap();
            Iterator iter = formElements.iterator();
            while (iter.hasNext()) {
                Object ob = iter.next();
                if (ob instanceof FormElement) {
                    FormElement fe = (FormElement) ob;
                    if (map.containsKey(fe.getName())) {
                        fe.setValue(((String[]) map.get(fe.getName()))[0]);
                    }
                }
            }
            if (map.containsKey("autosubmit")) {
                wasAutoSubmitted = true;
            }
            evaluate();
            getPage().updateHashURL();
            getPage().getListenerContainer().interrupt();
        }
    }

    /**
     * Is called after the form was submitted.
     */
    public abstract void evaluate();
    private static transient Template FORM = new Template(Form.class.getResource("form.html"));

    private static final Logger LOG = getLogger(Form.class.getName());
}
