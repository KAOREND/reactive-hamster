package com.kaibla.hamster.components.pageswitch;

import com.kaibla.hamster.base.Action;
import com.kaibla.hamster.base.HamsterAnimation;
import com.kaibla.hamster.base.HamsterComponent;
import static com.kaibla.hamster.base.UIEngine.addError;
import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.components.Condition;
import com.kaibla.hamster.components.Function;
import com.kaibla.hamster.collections.StringSource;
import com.kaibla.hamster.components.container.Container;
import com.kaibla.hamster.util.HTMLCodeFilter;
import static com.kaibla.hamster.util.HTMLCodeFilter.getStrictFilteredString;
import com.kaibla.hamster.util.Template;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * This component can be used as menue.
 *
 * @author Kai Orend
 */
public class PageSwitch extends HamsterComponent {

    LinkedList<Switch> switches = new LinkedList();
    Template template = null;
    PageSwitch self = this;
    private Template switchTemplate = null;
    private Template selectedSwitchTemplate = null;
    private Template divider = null;
    Container c = null;
    HashMap switchMap = new HashMap();
    String name;
    Switch selectedSwitch = null;
    Switch lastSelectedSwitch = null;
    String staticParent;
    boolean onMouseOver = false;
    private String cssClass = "";
    private String switchClass = "";
    private String selectedSwitchClass = "";
    /**
     * True if the position for the a tag in the switch template is not at the same position as the link text.
     */
    private boolean separateLinkTag = false;

    public PageSwitch() {
        super();
    }

    public PageSwitch(HamsterPage page, Container c, String name) {
        super(page);
        this.c = c;
        this.name = name;
        switchTemplate = SWITCH;
        selectedSwitchTemplate = SELECTED;
        template = PAGE_SWITCH;
    }

    public void setDivider(Template divider) {
        this.divider = divider;
    }

    /*
     *
     * Set the name of the static parent, which is used to generate static urls.
     *
     */
    public PageSwitch setStaticParent(String staticParent) {
        this.staticParent = staticParent;
        return this;
    }

    public void setUnactive() {
        if (selectedSwitch != null) {
            if (selectedSwitch.updateComponent != null) {
                selectedSwitch.updateComponent.markForUpdate();
            }
            lastSelectedSwitch = selectedSwitch;
            selectedSwitch = null;

            markForUpdate();
        }
    }

    public Switch getSelectedSwitch() {
        return selectedSwitch;
    }

    public void updateContainer() {
        if (lastSelectedSwitch != null) {
            if (lastSelectedSwitch.cf != null) {
                activateSwitch(lastSelectedSwitch);
            }
        }
    }

    @Override
    public void generateHTMLCode() {
        LinkedList slots = new LinkedList();
        String s = "";
        Iterator iter = switches.iterator();
        boolean first = true;
        while (iter.hasNext()) {
            Switch sw = (Switch) iter.next();

            if (sw.getCondition() == null || sw.getCondition().isAllowed()) {
                if (first) {
                    first = false;
                } else {
                    if (divider != null) {
                        s += divider.getString(getPage());
                    }
                }
//                if (sw.getCustomComp() != null) {
//
//                    s += sw.getCustomComp().getHTMLCode();
//                } else {
                    s += getSwitchCode(sw, this);
//                }
            }

        }
        slots.add("" + getId());
        if (PAGE_SWITCH == template) {
            slots.add(cssClass);
        }
        slots.add(s);
        htmlCode = template.mergeStrings(slots, getPage());
    }

    /**
     *
     * @param name The name of the Switch
     * @return The HTML Code for a link to activate this switch, which can be used in other components.
     */
    public String getSwitchCode(String name, HamsterComponent parent) {
        return getSwitchCode((Switch) switchMap.get(name), parent);
    }

    public Switch getSwitch(String name) {
        return (Switch) switchMap.get(name);
    }

    public LinkedList<Switch> getSwitches() {
        return switches;
    }

    
    public Action getSwitchAction(String name) {
        Switch sw = getSwitch(name);
        if (sw == null) {
            return null;
        }
        return new SwitchAction(sw);
    }

   
    
    

    public String getSwitchCode(Switch sw, HamsterComponent parent) {
        String s = "";
        String actualSwitchClass = this.switchClass;
        if (sw.getCssClass() != null) {
            actualSwitchClass = sw.getCssClass();
        }
        if (sw.getCondition() == null || sw.getCondition().isAllowed()) {
            if (selectedSwitch == sw) {
                if (separateLinkTag) {
                    LinkedList slots = new LinkedList();
                    slots.add(parent.getActionLinkTag(new SwitchAction(sw)));
                    slots.add(sw.getLabel());
                    if (sw.customTemplate != null) {
                        s += sw.customTemplate.mergeStrings(slots, getPage());
                    } else {
                        s += getSelectedSwitchTemplate().mergeStrings(slots, getPage());
                    }

                } else {
                    if (sw.cf == null) {
                        if (sw.customTemplate != null) {
                            s += sw.customTemplate.mergeStrings(parent.
                                    getActionLinkTag(new SwitchAction(sw), "class=\"" + actualSwitchClass + "\"") + sw.
                                    getLabel() + "</a>", getPage());
                        } else {
                            s += getSwitchTemplate().mergeStrings(parent.
                                    getActionLinkTag(new SwitchAction(sw), "class=\"" + actualSwitchClass + "\"") + sw.
                                    getLabel() + "</a>", getPage());
                        }
                    } else {
                        if (sw.customTemplate != null) {
                            s += sw.customTemplate.mergeStrings(sw.getLabel(), getPage());
                        } else if (selectedSwitch != null) {
                            s += getSelectedSwitchTemplate().mergeStrings(sw.
                                    getLabel(), getPage());
                        } else {
                            s = "<span class=\"" + selectedSwitchClass + "\">" + s + "</span>";
                        }
                    }

                }
            } else {
                if (onMouseOver) {
                    if (separateLinkTag) {
                        LinkedList slots = new LinkedList();
                        slots.add(parent.getOnMouseOverActionLinkTag(new SwitchAction(sw)));
                        slots.add(sw.getLabel());
                        if (sw.customTemplate != null) {
                            s += sw.customTemplate.mergeStrings(slots, getPage());
                        } else {
                            s += getSwitchTemplate().mergeStrings(slots, getPage());
                        }
                    } else {
                        if (sw.customTemplate != null) {
                            s += sw.customTemplate.mergeStrings(parent.
                                    getOnMouseOverActionLinkTag(new SwitchAction(sw)) + sw.
                                    getLabel() + "</a>", getPage());
                        } else {
                            s += getSwitchTemplate().mergeStrings(parent.
                                    getOnMouseOverActionLinkTag(new SwitchAction(sw)) + sw.
                                    getLabel() + "</a>", getPage());
                        }
                    }
                    // s += switchTemplate.mergeStrings(getActionLinkTag(new SwitchAction(sw), "onmouseover=\"hamster.main.doRequestAuxiliary('" + getActionURL(new SwitchAction(sw)) + "')\"") + sw.label + "</a>", page);
                } else {
                    if (separateLinkTag) {
                        LinkedList slots = new LinkedList();
                        slots.add(parent.getActionLinkTag(new SwitchAction(sw), "class=\"" + actualSwitchClass + "\""));
                        slots.add(sw.getLabel());
                        if (sw.customTemplate != null) {
                            s += sw.customTemplate.mergeStrings(slots, getPage());

                        } else {
                            s += getSwitchTemplate().mergeStrings(slots, getPage());
                        }

                    } else {
                        if (sw.customTemplate != null) {
                            s += sw.customTemplate.mergeStrings(parent.
                                    getActionLinkTag(new SwitchAction(sw), "class=\"" + actualSwitchClass + "\"") + sw.
                                    getLabel() + "</a>", getPage());
                        } else {
                            s += getSwitchTemplate().mergeStrings(parent.
                                    getActionLinkTag(new SwitchAction(sw), "class=\"" + actualSwitchClass + "\"") + sw.
                                    getLabel() + "</a>", getPage());
                        }
                    }
                }
            }
        }
        return s;
    }
    /*
     * Activates the onmouseover activation of the switches.
     *
     */

    public void setOnMouseOver(boolean onMouseOver) {
        this.onMouseOver = onMouseOver;
    }

    public boolean isOnMouseOver() {
        return onMouseOver;
    }

    public PageSwitch activateSwitch(String name) {

        Switch sw = (Switch) switchMap.get(name);
        if(sw == null) {
            return this;
        }
        if(sw == selectedSwitch) {
            //do nothing if switch is already selected
            return this;
        }
        if (sw.getCondition() == null || sw.getCondition().isAllowed()) {
            if (sw == null) {
                addError("Switch not found: " + name);
            }
            invokeAction(new SwitchAction(sw));
            getPage().updateHashURL();
        }
        return this;
    }

    public void deactivateSwitch() {
        if (selectedSwitch != null) {
            if (selectedSwitch.getComponenFactory() != null) {
                c.setComponent(null);
            } else {
                selectedSwitch.leave();
            }
        }
    }

    public PageSwitch activateSwitch(Switch sw) {
        if (!sw.isActive()) {
            if (selectedSwitch != null) {
                selectedSwitch.leave();
            }

            if (sw != null) {
                sw.invoke();
            }
            selectedSwitch = sw;            
            markForUpdate();
        }
        return this;
    }

    public PageSwitch setTemplate(Template template) {
        this.template = template;
        markForUpdate();
        return this;
    }

    public Switch addSwitch(String name, final String label) {
        return addSwitch(name, label, true);
    }

    public Switch addSwitch(String name, final String label, boolean encode) {
        return addSwitch(name, new StringSource() {

            @Override
            public String toString() {
                return label;
            }
        }, encode);
    }

    public Switch addSwitch(String name, StringSource label) {
        return addSwitch(name, label, true);
    }

    public Switch addSwitch(String name, final StringSource label, boolean encode) {
        Switch s = new Switch();
        if (encode) {
            s.label = new StringSource() {

                @Override
                public String toString() {
                    return getStrictFilteredString(label.toString());
                }
            };

        } else {
            s.label = label;
        }
        s.name = name;
        switches.add(s);
        switchMap.put(name, s);
        return s;
    }

    /**
     *
     * @param name
     * @param label
     * @param cf
     * @return
     * @deprecated
     */
    public PageSwitch addSwitch(String name, String label, ComponentFactory cf) {
        return addSwitch(name, label, cf, null, getSwitchTemplate());
    }

    public PageSwitch addSwitch(String name, String label, ComponentFactory cf, Condition cond) {
        return addSwitch(name, label, cf, cond, getSwitchTemplate());
    }

    public PageSwitch addSwitch(String name, String label, ComponentFactory cf, Condition cond, Template customTemplate) {
        Switch cw = new Switch();
        cw.name = name;
        cw.setLabel(label);
        cw.setComponentFactory(cf);
        cw.setCondition(cond);
        cw.setCustomTemplate(customTemplate);
        switches.add(cw);
        switchMap.put(name, cw);
        return this;
    }

    public PageSwitch addSwitch(String name, String label, Function activate, Function leave) {
        return addSwitch(name, label, activate, leave, null, getSwitchTemplate());
    }

    public PageSwitch addSwitch(String name, String label, Function activate, Function leave, Condition cond) {
        return addSwitch(name, label, activate, leave, cond, getSwitchTemplate());
    }

    public PageSwitch addSwitch(String name, String label, Function activate, Function leave, Condition cond, Template customTemplate) {
        Switch cw = new Switch();
        cw.name = name;
        cw.setLabel(label);
        cw.setInvokeFunction(activate);
        cw.setLeaveFunction(leave);
        cw.setCondition(cond);
        cw.setCustomTemplate(customTemplate);
        switches.add(cw);
        switchMap.put(name, cw);
        return this;
    }

    public void removeSwitch(String name) {
        Switch s = (Switch) switchMap.remove(name);
        if (s != null) {
            switches.remove(s);
        }
    }

    @Override
    public void updateAll() {
        super.updateAll();
        Iterator iter = switches.iterator();
        while (iter.hasNext()) {
            Switch sw = (Switch) iter.next();
            if (sw.getComponenFactory() != null) {
                sw.getComponenFactory().rebuild();
            }
            if (sw.f != null) {
                sw.f.rebuild();
            }
        }
        activateSwitch(selectedSwitch);
        super.updateAll();
    }

//    /**
//     * This method can be used to render a Switch with a custom component. This can for instance be used for submenues.
//     *
//     * @param name The name of the Switch
//     * @param comp The Component that should be used to Render the Switch
//     */
//    public void setCustomSwitch(String name, HamsterComponent comp) {
//        Switch sw = (Switch) switchMap.get(name);
//        if (sw.getCustomComp() != null) {
//            remove(sw.getCustomComp());
//        }
//        if (sw.f != null) {
//            sw.f = null;
//        }
//        sw.setCustomComp(comp);
//        if (comp != null) {
//            addComponent(comp);
//        }
//
//    }

//    /**
//     * This method can be used to render a Switch with a custom component. This can for instance be used for submenues.
//     *
//     * @param name The name of the Switch
//     * @param f The factory to build the custom component
//     */
//    public void setCustomSwitchFactory(String name, ComponentFactory f) {
//        Switch sw = (Switch) switchMap.get(name);
//        if (sw.getCustomComp() != null) {
//            remove(sw.getCustomComp());
//        }
//        sw.setCustomCompFactory(f);
//        if (sw.getCustomComp() != null) {
//            addComponent(sw.getCustomComp());
//        }
//    }

    /**
     * @return the switchTemplate
     */
    public Template getSwitchTemplate() {
        return switchTemplate;
    }

    /**
     * @param switchTemplate the switchTemplate to set
     */
    public PageSwitch setSwitchTemplate(Template switchTemplate) {
        this.switchTemplate = switchTemplate;
        return this;
    }

    /**
     * @return the selectedSwitchTemplate
     */
    public Template getSelectedSwitchTemplate() {
        return selectedSwitchTemplate;
    }

    /**
     * @param selectedSwitchTemplate the selectedSwitchTemplate to set
     */
    public PageSwitch setSelectedSwitchTemplate(Template selectedSwitchTemplate) {
        this.selectedSwitchTemplate = selectedSwitchTemplate;
        return this;
    }

    /**
     * True if the position for the a tag in the switch template is not at the same position as the link text.
     */
    public boolean isSeparateLinkTag() {
        return separateLinkTag;
    }

    /**
     * @param separateLinkTag the separateLinkTag to set
     */
    public PageSwitch setSeparateLinkTag(boolean separateLinkTag) {
        this.separateLinkTag = separateLinkTag;

        return this;
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
    public PageSwitch setCssClass(String cssClass) {
        this.cssClass = cssClass;
        return this;
    }

    /**
     * @return the switchClass
     */
    public String getSwitchClass() {
        return switchClass;
    }

    /**
     * @param switchClass the switchClass to set
     */
    public void setSwitchClass(String switchClass) {
        this.switchClass = switchClass;
    }

    /**
     * @return the selectedSwichtClass
     */
    public String getSelectedSwitchClass() {
        return selectedSwitchClass;
    }

    /**
     * @param selectedSwichtClass the selectedSwichtClass to set
     */
    public void setSelectedSwitchClass(String selectedSwichtClass) {
        this.selectedSwitchClass = selectedSwichtClass;
    }

    public class Switch implements Serializable {

        String name;
        private StringSource label;
        private Template customTemplate = null;
        private Condition cond;
        private HamsterComponent customComp;
        private ComponentFactory cf;
        private Function invokeFunction;
        private Function leaveFunction;
        private ComponentFactory f;
        private HamsterAnimation anim;
        private HamsterComponent updateComponent;
        private String cssClass;
        private Object htmlStart = "";
        private Object htmlEnd = "";
        private boolean translateLabel = true;
        private String animation = null;

        public PageSwitch getParent() {
            return self;
        }

        public Switch setAnimation(String animation) {
            this.animation = animation;
            return this;
        }

        public String getAnimation() {
            return animation;
        }
        
        
        
        

        public void setUpdateComponent(HamsterComponent updateComponent) {
            this.updateComponent = updateComponent;
        }

        public void setAnim(HamsterAnimation anim) {
            this.anim = anim;
        }

        public void invoke() {
            if (cond == null || cond.isAllowed()) {
                if (invokeFunction != null) {
                    invokeFunction.invoke();
                }
                if (getComponenFactory() != null) {
                    if (anim != null) {
                        c.setAnimation(anim);
                        c.markForAnimation();
                    }
                    c.setComponent(getComponenFactory().getComponent(), self);
                    page.setTitle(HTMLCodeFilter.removeAllHTML(getLabelText()));
                }
            }
            if (updateComponent != null) {
                updateComponent.markForUpdate();
            }
        }

        public Switch setTranslateLabel(boolean translateLabel) {
            this.translateLabel = translateLabel;
            return this;
        }

        public Switch setHtmlEnd(String htmlEnd) {
            this.htmlEnd = htmlEnd;
            return this;
        }

        public Switch setHtmlStart(String htmlStart) {
            this.htmlStart = htmlStart;
            return this;
        }

        public Switch setHtmlEnd(StringSource htmlEnd) {
            this.htmlEnd = htmlEnd;
            return this;
        }

        public Switch setHtmlStart(StringSource htmlStart) {
            this.htmlStart = htmlStart;
            return this;
        }

        /**
         * Checks if this PageSwitch is still active
         *
         * @return true if the PageSwitch is active
         */
        public boolean isActive() {
            if (cf != null) {
                if (c != null && cf.getComponent() != null && cf.getComponent() == c.
                        getComponent()) {
                    return (invokeFunction == null) || (invokeFunction.isActive());
                }
            } else if (cf == null && invokeFunction != null) {
                return invokeFunction.isActive();
            }
            return false;
        }

        public String getName() {
            return name;
        }

        public void leave() {
            if (leaveFunction != null) {
                leaveFunction.invoke();
            }
            if (updateComponent != null) {
                updateComponent.markForUpdate();
            }
        }

        public Switch setCssClass(String cssClass) {
            this.cssClass = cssClass;
            return this;
        }

        public String getCssClass() {
            return cssClass;
        }

        /**
         * @return the label
         */
        public String getLabel() {
            if (translateLabel) {
                return htmlStart + getTranslatedString(label + "") + htmlEnd;
            } else {
                return ""+htmlStart + label  + htmlEnd;
            }
        }
        
        public String getLabelText() {
             if (translateLabel) {
                return  getTranslatedString(label + "") ;
            } else {
                return ""+label;
            }
        }

        /**
         * @param label the label to set
         */
        public Switch setLabel(StringSource label) {
            this.label = label;
            return this;
        }

        /**
         * @param label the label to set
         */
        public Switch setLabel(final String label) {
            this.label = new StringSource() {

                @Override
                public String toString() {
                    return label;
                }
            };
            return this;
        }

        /**
         * @return the customTemplate
         */
        public Template getCustomTemplate() {
            return customTemplate;
        }

        /**
         * @param customTemplate the customTemplate to set
         */
        public Switch setCustomTemplate(Template customTemplate) {
            this.customTemplate = customTemplate;
            return this;
        }

        /**
         * @return the cond
         */
        public Condition getCondition() {
            return cond;
        }

        /**
         * @param cond the cond to set
         */
        public Switch setCondition(Condition cond) {
            this.cond = cond;
            return this;
        }

//        /**
//         * @return the customComp
//         */
//        public HamsterComponent getCustomComp() {
//            if (f != null) {
//                HamsterComponent c = f.getComponent();
//                if (c != customComp) {
//                    setCustomComp(c);
//                }
//            }
//            return customComp;
//        }

//        /**
//         * @param customComp the customComp to set
//         */
//        public Switch setCustomComp(HamsterComponent customComp) {
//            if (this.customComp != null) {
//                self.remove(this.customComp);
//            }
//            this.customComp = customComp;
//
//            if (customComp != null) {
//                self.addComponent(customComp);
//            }
//            return this;
//        }

//        /**
//         * @param customComp the customComp to set
//         */
//        public Switch setCustomCompFactory(ComponentFactory f) {
//            this.f = f;
//            setCustomComp(f.getComponent());
//            return this;
//        }

        /**
         * @return the cf
         */
        public ComponentFactory getComponenFactory() {
            return cf;
        }

        /**
         * @param cf the cf to set
         */
        public Switch setComponentFactory(ComponentFactory cf) {
            this.cf = cf;
            return this;
        }

        /**
         * @param f the f to set
         */
        public Switch setInvokeFunction(Function f) {
            this.invokeFunction = f;
            return this;
        }

        /**
         * @param l the l to set
         */
        public Switch setLeaveFunction(Function l) {
            this.leaveFunction = l;
            return this;
        }
    }

    public class SwitchAction extends Action {

        Switch sw;
        Switch old;
        HamsterComponent o;

        public SwitchAction() {
        }

        private SwitchAction(Switch sw) {
            this.sw = sw;
            setAnimation(sw.animation);
        }

        @Override
        public void invoke() {
            if (sw.getCondition() == null || sw.getCondition().isAllowed()) {
                old = selectedSwitch;
                if (c != null) {
                    o = c.getComponent();
                }
                activateSwitch(sw);
            }
        }

        @Override
        public String getStaticParent() {
            return staticParent;
        }

        @Override
        public boolean isAllowed() {
            return sw != null && (sw.cond == null || sw.cond.isAllowed());
        }

        @Override
        public String getStaticName() {
            return name;
        }

        @Override
        public String getStaticParameter() {
            return sw.name;
        }

        @Override
        public void setStaticParameter(String s) {
            sw = (Switch) switchMap.get(s);
        }

        @Override
        public void Undo() {
            if (o != null) {
                c.setComponent(o);
            } else if (old != null) {
                activateSwitch(old);
            }

        }

        @Override
        public boolean hasUndo() {
            return true;
        }
    }

    public static String encodeSwitchName(String name) {
        if (name == null) {
            return "";
        }
        name = name.toLowerCase();
        name = decode(name);
        name = name.replaceAll(" ", "");
        name = name.trim();
        try {
            name = URLEncoder.encode(name, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
          LOG.log(Level.SEVERE, null, ex);
        }
        name = name.replaceAll("%", "");
        //LOG.info("encodeSwitchName "+name);
        return name;
    }
    private static transient Template SELECTED = new Template(PageSwitch.class.getResource("selected.html"));
    private static transient Template SWITCH = new Template(PageSwitch.class.getResource("switch.html"));
    private static transient Template PAGE_SWITCH = new Template(PageSwitch.class.
            getResource("pageswitch.html"));
    private static final Logger LOG = getLogger(PageSwitch.class.getName());
}
