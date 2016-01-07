package com.kaibla.hamster.components.defaultcomponent;

import com.kaibla.hamster.base.Action;
import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.components.Function;
import com.kaibla.hamster.collections.StringSource;
import com.kaibla.hamster.util.Template;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class DefaultComponent extends HamsterComponent {

    protected ArrayList elements = new ArrayList();
    //functions that will be executed everytime the page is beeing rerendered
    protected ArrayList<Function> regenerationFunctions = new ArrayList<Function>();
    protected Template template;
    protected boolean oneSlot = true;

    public DefaultComponent() {
        template = DEFAULT;
        elements.add("" + getId());
    }

    public DefaultComponent(HamsterPage page) {
        super(page);
        template = DEFAULT;
        elements.add("" + getId());
    }

    public DefaultComponent(HamsterPage page, boolean oneSlot, Template template) {
        super(page);
        elements.add("" + getId());
        this.oneSlot = oneSlot;
        this.template = template;
    }
    
    public void addRegenerationFunction(Function function) {
        regenerationFunctions.add(function);
    }

    @Override
    public void generateHTMLCode() {
        if (isOneSlot()) {
            LinkedList slots = new LinkedList();
            for(Function function : regenerationFunctions) {
                function.invoke();
            }
            Iterator iter = elements.iterator();
            slots.add(iter.next());
            String s = "";
            while (iter.hasNext()) {
                Object o = iter.next();
                if (o instanceof Action) {
                    s += getActionLinkTag((Action) o);
                } else {
                    s += o;
                }
            }
            slots.add(s);
            htmlCode = template.mergeStrings(slots, getPage());
        } else {
            for(Function function : regenerationFunctions) {
                function.invoke();
            }
            LinkedList slots = new LinkedList();
            Iterator iter = elements.iterator();
            while (iter.hasNext()) {
                Object o = iter.next();
                if (o instanceof Action) {
                    slots.add(getActionLinkTag((Action) o));
                } else {
                    slots.add(o);
                }
            }
            htmlCode = template.mergeStrings(slots, getPage());
        }

    }

    public DefaultComponent addElement(String s) {
        elements.add(s);
        markForUpdate();
        return this;
    }

    public DefaultComponent addElement(Object o) {
        elements.add(o);
        markForUpdate();
        return this;
    }

    public DefaultComponent addElement(StringSource source) {
        elements.add(source);
        markForUpdate();
        return this;
    }

    public DefaultComponent addAction(Action a) {
        elements.add(a);
        markForUpdate();
        return this;
    }

    public DefaultComponent addElement(HamsterComponent comp) {
        elements.add(comp);
        addComponent(comp);
        markForUpdate();
        return this;
    }

    public DefaultComponent removeElement(HamsterComponent comp) {
        super.removeAndDestroy(comp);
        elements.remove(comp);
        markForUpdate();
        return this;
    }

    @Override
    public void removeAndDestroyAll() {
        super.removeAndDestroyAll();
        elements.clear();
        elements.add("" + getId());
    }

    @Override
    public void remove(HamsterComponent comp) {
        super.remove(comp);
        elements.remove(comp);
    }

    @Override
    public void removeAndDestroy(HamsterComponent comp) {
        super.removeAndDestroy(comp);
        elements.remove(comp);
    }

    public DefaultComponent setTemplate(Template template) {
        this.template = template;
        if (template == null) {
            this.template = DEFAULT;
        }
        markForUpdate();
        return this;
    }

    private static transient Template DEFAULT = new Template(DefaultComponent.class.getResource("default.html"));

    /**
     * @return the oneSlot
     */
    public boolean isOneSlot() {
        return oneSlot;
    }

    /**
     * @param oneSlot the oneSlot to set, if true the Elements will all be put into one slot, instead of putting every
     * element in its own slot.
     */
    public DefaultComponent setOneSlot(boolean oneSlot) {
        this.oneSlot = oneSlot;
        return this;
    }    
    
    private static final Logger LOG = getLogger(DefaultComponent.class.getName());

}
