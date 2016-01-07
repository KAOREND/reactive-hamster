/*
 * HamsterComponent.java Created on 18. Februar 2007, 20:33
 */
package com.kaibla.hamster.base;

import static com.kaibla.hamster.base.UIEngine.getStaticAction;
import static com.kaibla.hamster.base.UIEngine.registerStaticAction;
import com.kaibla.hamster.base.Reconstructor.Element;
import com.kaibla.hamster.collections.StringSource;
import com.kaibla.hamster.servlet.CometProcessor;
import static com.kaibla.hamster.util.HTMLCodeFilter.getStrictFilteredString;
import com.kaibla.hamster.util.CloneMap;
import com.kaibla.hamster.util.HTMLCodeFilter;
import com.kaibla.hamster.util.HamsterCloneable;
import com.kaibla.hamster.util.Localizer;
import java.io.Serializable;
import static java.lang.Integer.parseInt;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * @author Kai Orend
 */
public abstract class HamsterComponent extends AbstractListenerOwner implements ChangedListener, HamsterCloneable, Serializable {

    private static final long serialVersionUID = 1L;
    transient boolean debug = true;
    public ArrayList<HamsterComponent> components = new ArrayList();
    private boolean needsUpdate = true;
    public transient String htmlCode = null;
    public HamsterComponent parent = null;

    
    private HamsterAnimation animation = null;
    private transient boolean markedForAnimation = false;
    private transient boolean isInBrowserCache = false;
    protected boolean addedToParent = false;
    private ArrayList<Action> actions = new ArrayList();
    private Action lastStaticAction = null;
    protected boolean visible = false;
    //lock system used by fileupload to avoid ajax freshes while uploading

    private boolean alwaysAnimate = false;
    protected HashMap<Class, HamsterComponent> serviceMap;
    protected ArrayList<HamsterComponent> reverseServiceRegistrations;
    private transient boolean destroyed = false;
    protected HamsterPage page;
    protected int id = -1;
    
    

    public HamsterComponent() {        
        this.page = UIContext.getPage();
        if(this instanceof HamsterPage) {
            this.page=(HamsterPage) this;
        }
        setPage(page);
        CometProcessor.getEngine().getHamsterLoader().loadComponent(this.getClass());
    }

    /**
     * Creates a new instance of HamsterComponent
     */
    public HamsterComponent(HamsterPage page) {     
        this.page = page;
        setPage(page);
        if(this instanceof HamsterPage) {
            this.page=(HamsterPage) this;
        }
        page.getEngine().getHamsterLoader().loadComponent(this.getClass());
    }

    @Override
    public boolean isDestroyed() {
        if (page != null && page.isDestroyed()) {
            return true;
        } else if (page == null) {
            return true;
        }
        return destroyed;
    }

    public final void setPage(HamsterPage page) {
        CometProcessor.getEngine().addComponent(this);
        components = new ArrayList();
        setListenerContainer(page.getListenerContainer());
        actions = new ArrayList();
    }

 

    public final void exec(String jscript) {
        LOG.fine("execJS: " + jscript);
        getPage().getModificationManager().executeScriptOnClient(jscript);
    }

    public final void execFirst(String jscript) {
        LOG.fine("execJS: " + jscript);
        getPage().getModificationManager().executeScriptOnClientFirst(jscript);
    }

    public void updateAll() {
        if (!(this instanceof HamsterPage)) {
            markForUpdate();
        }
        for (HamsterComponent comp : components) {
            comp.updateAll();
        }
    }

    public void debugStaticActions() {
        if (getLastStaticAction() != null) {
        } else {
            // LOG.info("no static Action:
            // "+this.getClass().getName());
        }
        for (HamsterComponent comp : components) {
            comp.debugStaticActions();
        }
    }

    public void reconstruct(Reconstructor rec) {
        LinkedList list = rec.list();
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            Element e = (Element) iter.next();
            Action action = getStaticAction(e.getName(), this);
            if (action != null) {
                if (e.getParam() != null) {
                    action.setStaticParameter(e.getParam());
                }
                if (action.isAllowed()) {
//                    setLastStaticAction(action);
//                    page.registerStaticAction(action, this);
                    iter.remove();
//                    action.invoke(null, null);
                    invokeAction(action);
                    break;
                }
            }
        }

        reconstructRec(rec);
    }

    private void reconstructRec(Reconstructor rec) {
        Iterator iter = new LinkedList(components).iterator();
        while (iter.hasNext()) {
            try {
                HamsterComponent comp = (HamsterComponent) iter.next();
                comp.reconstruct(rec);
            } catch (ConcurrentModificationException ex) {
                reconstructRec(rec);
            }
        }
    }

    /**
     * Is invoked on Server StartUp.
     */
    public static void onClassLoad(UIEngine engine) {
    }

    

    public void confirmCache() {
        //komponente kann nur im cache sein, wenn sie auch wirklich gerendert wurde
        if (htmlCode != null && visible) {
            isInBrowserCache = true;
            htmlCode = null;
            for (HamsterComponent comp : components) {
                if (comp != null) {
                    comp.confirmCache();
                }
            }
        }
    }

    public boolean isInBrowserCache() {
        return isInBrowserCache;
    }

    public HamsterPage getPage() {
        if (page == null) {
            return UIContext.getPage();
        }
        return page;
    }

    /*
     * public String getLocal(String key) { return getLanMan().getLocal(key,
     * page.getLanguage()); }
     */
    public void addComponent(HamsterComponent comp) {
        if (comp == null) {
            return;
        }
        if (comp.getLastStaticAction() != null) {
            getPage().registerStaticAction(comp.getLastStaticAction(), this);
        }
        components.add(comp);
        comp.parent = this;
        comp.visible = visible;
        comp.registerAllStaticActions();
        if (addedToParent) {
            comp.markAsAddedToParent();
        }
        markForUpdate();
    }

    private void markAsAddedToParent() {
        if (!addedToParent) {
            addedToParent = true;
            init();
            for (HamsterComponent comp : components) {
                comp.markAsAddedToParent();
            }
        }
    }

    public void init() {
    }

    protected boolean isInTreeOf(HamsterComponent comp) {
        if (this == comp) {
            return true;
        }
        if (this.getParent() != null) {
            return this.getParent().isInTreeOf(comp);
        } else {
            return false;
        }
    }

    public void replaceComponent(HamsterComponent comp, HamsterComponent replacement) {
        components.add(components.indexOf(comp), replacement);
        removeAndDestroy(comp);
        replacement.parent = this;
        replacement.registerAllStaticActions();
        markForUpdate();
        if (components.contains(replacement)) {
            throw new RuntimeException("replacement was not added to the components");
        }
    }

    /*
     * public String getLocal(String key) { return getLanMan().getLocal(key,
     * page.getLanguage()); }
     */
    public void addComponentFirst(HamsterComponent comp) {
        if (comp.getLastStaticAction() != null) {
            getPage().registerStaticAction(comp.getLastStaticAction(), this);
        }
        components.add(0, comp);
        comp.parent = this;
        comp.registerAllStaticActions();
        if (addedToParent) {
            comp.markAsAddedToParent();
        }
        markForUpdate();
        getPage().getModificationManager().appendChild(comp);
    }

    /**
     * Does the same as addComponent, but comp is appended in the browser as last child of this component. This
     * component will not be reloaded in the browser for doing this. Only the new child will be loaded on the client
     * side.
     */
    public void appendComponent(HamsterComponent comp) {
        components.add(comp);
        comp.parent = this;
        comp.registerAllStaticActions();
        comp.visible = visible;
        //markTreeForUpdate();
        if (addedToParent) {
            comp.markAsAddedToParent();
        }
        if (components.size() > 1) {
            getPage().getModificationManager().appendChild(comp);
        } else {
            markForUpdate();
        }
    }

    protected void unregisterAllStaticActions() {
        // LOG.info("HamsterComponent: unregisterAllStaticActions:
        // "+this.getClass().getName());
        if (getLastStaticAction() != null) {
            getPage().unregisterStaticAction(getLastStaticAction(), this);
        }
        for (HamsterComponent comp : components) {
            comp.unregisterAllStaticActions();
        }
    }

    protected void registerAllStaticActions() {
        // LOG.info("HamsterComponent: registerAllStaticActions:
        // "+this.getClass().getName());
        if (getLastStaticAction() != null) {
            getPage().registerStaticAction(getLastStaticAction(), this);
        }
        for (HamsterComponent comp : components) {
            comp.registerAllStaticActions();
        }
    }

    public void remove(HamsterComponent comp) {
        if ((comp != null) && (components != null)) {
            // comp.unregisterAllStaticActions();
            components.remove(comp);
            comp.parent = null;
            getPage().getModificationManager().removeComponent(comp);
            markForUpdate();
        }
    }

    public void removeAndDestroyAll() {

        for (HamsterComponent comp : components) {
            getPage().getModificationManager().removeComponent(comp);
        }

        Iterator iter = components.iterator();
        while (iter.hasNext()) {
            HamsterComponent comp = (HamsterComponent) iter.next();
            if (comp.getLastStaticAction() != null) {
                getPage().
                        unregisterStaticAction(comp.getLastStaticAction(), this);
            }
            comp.destroy();
        }
        components.clear();
        markForUpdate();
    }

    public void removeAndDestroy(HamsterComponent comp) {
        if (comp != null) {
            remove(comp);
            comp.destroy();
        }
    }

    public void generateHTMLCode() {
        htmlCode = "<h2>HamsterComponent</h2>";
    }

    public void prePersist() {
        isInBrowserCache = false;
    }

    public void afterResume() {
        isInBrowserCache = false;
        super.afterResume();
    }

    public String getHTMLCode() {
        // if (needsUpdate || (htmlCode == null)) {
        // generateHTMLCode();
        // needsUpdate = false;
        // }
        // return htmlCode;

        visible = true; //Komponenten wird von seinem parent gerendert
//        if(getParent() != null && !getParent().components.
//                contains(this)) {
//            getParent().addComponent(this);
//        }
        if (debug && ((getParent() == null) || !getParent().components.
                contains(this)) && !(this instanceof HamsterPage)) {
            // Fehler ausgabe:

            // Thread.dumpStack();
            return ("Error: HamsterComponent " + this.getClass().getName() + "with ID " + this.
                    getId() + " was not added with addComponent to its parent.");

        }
        if (isRerendered() && ! getPage().getModificationManager().isAlreadyInCurrentResult(this)) {
            //Kinder als nicht sichtbar markieren,
            //damti nur bei den sichtbaren  visible=true ist
            for (HamsterComponent comp : components) {
                comp.visible = false;
            }       
            actions.clear();
            generateHTMLCode();
            needsUpdate = false;
            getPage().getModificationManager().addRenderedComponent(this);
//       if(htmlCode.contains("ö") || htmlCode.contains("ä") || htmlCode.contains("ü") || htmlCode.contains("Ö") || htmlCode.contains("Ä") || htmlCode.contains("Ü") || htmlCode.contains("ß") || htmlCode.contains("%")) {
//           LOG.info("HamsterComponent: htmlCode contains forbidden characters in class: "+getClass().getName());
//           LOG.info(htmlCode);
//          Thread.dumpStack();
//       }
            String result = htmlCode;
            htmlCode = "";
//            return htmlCode;
            return result;
        } else {
            return "<cache id=\"" + getId() + "\"></cache>";
        }
    }

    public boolean isRerendered() {
        return needsUpdate || !isInBrowserCache;
    }



    public void onShow() {
    }

    public String getIFrameHTMLCode() {
        return "<body><h1>IFRAME</h1></body>";
    }

    public HamsterComponent getParent() {
        return parent;
    }

    /**
     * Clears the Buffer for the HTMLCode. This Method does not notify its parent.
     */
    public void clearBuffer() {
        needsUpdate = true;
        htmlCode = null;
        isInBrowserCache = false;
    }

    protected boolean isVisible() {
        if (parent == null) {
            return (this instanceof HamsterPage);
        }
        if (parent != getPage() && parent != this && visible) {
            return parent.isVisible();
        } else {
            return visible;
        }

    }

//    public void lock() {
//        locked = true;
//        if (parent != null && parent != this) {
//            parent.lock();
//        }
//    }
//
//    public void unlock() {
//        locked = false;
//        if (parent != null && parent != this) {
//            parent.unlock();
//        }
//        if (updateInLockedState) {
//            updateInLockedState = false;
//            markForUpdate();
//        }
//    }
    public void markForUpdate() {
        try {
            if (!page.getLock().tryLock(10, TimeUnit.SECONDS)) {
                Logger.getLogger(HamsterComponent.class.getName()).log(Level.SEVERE, "Could not aquire page lock after 10 seconds");
                return;
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(HamsterComponent.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        try {
            isInBrowserCache = false;
//            updateTree();          
            getPage().getModificationManager().addMod(this);
        } finally {
            page.getLock().unlock();
        }

    }

//    /**
//     * Sets needUpate to true for all parent components in order to avoid that this componenten
//     * is removed through optimization
//     */
//    private void updateTree() {
//        if(parent != this  && parent != null) {
//            parent.updateTree();
//        }
//        needsUpdate=true;
//    }
    public boolean isMarkedForAnimation() {
        return markedForAnimation || alwaysAnimate;
    }

    public void markForAnimation() {
        markedForAnimation = true;
        markForUpdate();
    }

    public void setAlwaysAnimate(boolean alwaysAnimate) {
        this.alwaysAnimate = alwaysAnimate;
    }

    public boolean isAlwaysAnimate() {
        return alwaysAnimate;
    }

    public void unmarkForAnimation() {
        markedForAnimation = false;
    }

    public void invokeAction(Action action) {
//        if (action.hasUndo()) {
//            getPage().getHistoryManager().addAction(action);
//        } else {
//            getPage().getHistoryManager().addStaticLink(getPage().
//                    getStaticPath());
//        }
        setLastStaticAction(action);
        getPage().getEngine().logAction(this, action);
        action.invoke();
    }

    public void handleActions(LinkedList params) {
        if (!params.isEmpty()) {

            int index = parseInt(params.getFirst().toString());
            if (index < actions.size()) {
                Action action = actions.get(index);
                if (action != null && action.isAllowed()) {
                    invokeAction(action);
                }
            } else {
            }
        }

    }

    public void setLastStaticAction(Action action) {
        if (action.getStaticName() != null) {
            if (getLastStaticAction() != null) {
                getPage().unregisterStaticAction(getLastStaticAction(), this);
            }
            lastStaticAction = action;
            getPage().registerStaticAction(action, this);
            getPage().setStaticPath(getPage().getStaticPath(action));
        }
    }

    private String generateMethodString(LinkedList params) {
        StringBuilder virtualPath = new StringBuilder();
        virtualPath.append(getPage().getId()).append("?").append(getId());
        if (params != null) {
            Iterator iter = params.iterator();
            while (iter.hasNext()) {
                virtualPath.append('?').append(iter.next().toString());
            }
        }
        return virtualPath.toString();
    }

    public String getXLink(LinkedList params) {
        return getPage().getEnginePath() + "?x?" + generateMethodString(params);
    }

    public String getHLink(LinkedList params) {
        return getPage().getEnginePath() + "?h?" + generateMethodString(params);
    }

    public String getCLink(LinkedList params) {
        return getPage().getEnginePath() + "?c?" + generateMethodString(params);
    }

    public String getXMLRequest(LinkedList params) {
        return "javascript:hamster.main.doRequestAuxiliary('" + getXLink(params) + "')";
    }

    public String getSubmitAction(LinkedList params, boolean resetAfterSubmit) {
        return "javascript:hamster.main.postForm('" + getXLink(params) + "','" + getPage().
                getId() + "?" + getId() + "'," + resetAfterSubmit + ")";
    }

    public String getSubmitAction(LinkedList params, String customFormId, boolean resetAfterSubmit) {
        return "javascript:hamster.main.postForm('" + getXLink(params) + "','" + customFormId + "'," + resetAfterSubmit + ")";
    }

    public String getSubmitAction(boolean resetAfterSubmit) {
        return "javascript:hamster.main.postForm('" + getXLink(null) + "','" + getPage().
                getId() + "?" + getId() + "'," + resetAfterSubmit + ")";
    }

    public String getIFrameURL() {
        String s = getPage().getEnginePath();
        s += "?i?" + getPage().getId() + "?" + getId();
        return s;
    }

    public String getSubmitAction(Action action, boolean resetAfterSubmit) {
        return "javascript:hamster.main.postForm('" + getSubmitActionURL(action) + "','" + getId() + "'," + resetAfterSubmit + ")";
    }

    public String getSubmitActionURL(Action action) {
        actions.add(action);
        String methodLink = "" + getPage().getId() + "?" + getId() + "?" + (actions.
                size() - 1);
        return getPage().getEnginePath() + "?x?" + methodLink;
    }

    public String getSubmitActionWithRTE(Action action, String editorId, String hiddenInputId, boolean resetAfterSubmit) {
        actions.add(action);
        String methodLink = "" + getPage().getId() + "?" + getId() + "?" + (actions.
                size() - 1);
        return "javascript:hamster.main.postFormWithRTE('" + getPage().
                getEnginePath() + "?x?" + methodLink + "','" + getId() + "','" + editorId + "','" + hiddenInputId + "'," + resetAfterSubmit + ")";
    }

    public String getSubmitAction(Action action, String customFormId, boolean resetAfterSubmit) {
        actions.add(action);
        String methodLink = "" + getPage().getId() + "?" + getId() + "?" + (actions.
                size() - 1);
        return "javascript:hamster.main.postForm('" + getPage().getEnginePath() + "?x?" + methodLink + "','" + customFormId + "'," + resetAfterSubmit + ")";
    }

    public String getActionURL(Action action) {
        registerStaticAction(action, this);
        actions.add(action);
        String methodLink = "" + getPage().getId() + "?" + getId() + "?" + (actions.
                size() - 1);
        return "./?x?" + methodLink;
    }

    public String getUploadAction(Action action) {
        actions.add(action);
        return getPage().getEnginePath() + "?u?" + getPage().getId() + "?" + getId() + "?" + (actions.
                size() - 1);
    }

    public String getOnMouseOverActionLinkTag(Action action, String params) {
        return getActionLinkTag(action, "onmouseover=\"hamster.main.doRequestAuxiliary('" + getActionURL(action) + "')\"");
    }

    public String getOnMouseOverActionLinkTag(Action action) {
        return getOnMouseOverActionLinkTag(action, "");
    }

    public String getActionLinkTag(Action action, String params) {
        return getActionLinkTag(action, params, getPage().getEnginePath());
    }

    /**
     * Generates the first part of an link tag for the given action.
     */
    public String getActionLinkTag(Action action, String params, String contextPath) {
        registerStaticAction(action, this);
        actions.add(action);
        getPage().addUnactiveStaticAction(action);
        String methodLink = "" + getPage().getId() + "?" + getId() + "?" + (actions.
                size() - 1);
        String staticPath = "";
        String klink;
        if (action.getStaticName() != null) {
            staticPath = getPage().getStaticPath(action);
            // use static link instead of clone link:
            klink = getPage().getRelativePath() + staticPath + ".hsp";
        } else {
            klink = contextPath + "?k?" + methodLink;
        }
        String xlink = "";
        if (action.hasUndo() || action.getStaticName() != null) {
            xlink = contextPath + "?a?" + methodLink;
        } else {
            xlink = contextPath + "?x?" + methodLink;
        }
        String id = "L" + getPage().getNewLinkId();
        String animation="";
        if(action.getAnimation() != null) {
            animation="data-animation=\""+action.getAnimation()+"\"";
        }
        return "<a id=\"" + id + "\" "+animation             
                +" onclick=\"hamster.main.changeToXLink('" + id + "','" + xlink + "','" + staticPath + "')\" href=\"" + klink + "\" " + params + ">";

        // return generateLinkTagWithMethodLink(getId() + "?" + count + "?" + (actions.size() - 1));
    }

    /**
     * Generates the html code for an Action link. Warning: This method should only be called inside generateHTMLCode of
     * this Component.
     *
     * @param action
     * @return
     */
    public String getActionLinkTag(Action action) {
        return getActionLinkTag(action, "");
    }

    public StringSource getActionLinkTagStringSource(final Action action) {
        return new StringSource() {
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                return getActionLinkTag(action);
            }
        };
    }

//    private String generateLinkTagWithMethodLink(String methodLink) {
//
//        String klink = page.getRelativePath() + "?k?" + methodLink;
//        // String
//        // mlink="./"+page.getEngine().getServlet().getServletName()+"?m?"+methodLink;
//        String xlink = page.getRelativePath() + "?x?" + methodLink;
//        String id = "L" + page.getNewLinkId();
//        return "<a id=\"" + id + "\" onclick=\"hamster.main.changeToXLink('" + id + "','" + xlink + "')\" href=\"" + klink + "\">";
//    }
    public String getStaticLinkTag(String staticPath) {
        String id = "L" + getPage().getNewLinkId();
        return "<a id=\"" + id + "\" onclick=\"hamster.main.changeToZLink('" + id + "','" + staticPath + "')\" href=\"./" + staticPath + "\">";
    }

    public void destroy() {
        if (getPage().getFocusedElement() != null && getPage().getFocusedElement().isInTreeOf(this)) {
            getPage().setFocusedElement(null);
        }
        if (reverseServiceRegistrations != null) {
            for (HamsterComponent comp : reverseServiceRegistrations) {
                comp.serviceMap.remove(this.getClass());
            }
        }
        if (getLastStaticAction() != null) {
            getPage().unregisterStaticAction(getLastStaticAction(), this);
        }
        getPage().removeOnShowListener(this);
         for (HamsterComponent comp : components) {
            comp.destroy();
        }
        super.destroy();
        getPage().getEngine().removeComponent(this);
        visible = true;
        destroyed = true;
    }

//    @Override
//    public int hashCode() {
//        getId();
//        return id;
//    }
    @Override
    public String toString() {
        return getHTMLCode();
    }

    @Override
    public void dataChanged(DataEvent e) {
        markForUpdate();
    }

    public Object clone(CloneMap map) {
        return map.getClone(this);
    }

    public void copyFrom(HamsterComponent orig, CloneMap map) {
        map.put(orig, this);
        isInBrowserCache = false;
        super.copyFrom(orig, map.getMap());
    }

    public <T> T findComponent(Class<T> type) {
        if (type.isAssignableFrom(this.getClass())) {
            return (T) this;
        } else if (serviceMap != null && serviceMap.containsKey(type)) {
            return (T) serviceMap.get(type);
        } else if (this instanceof HamsterPage) {
            return null;
        } else if (getParent() != null) {
            return getParent().findComponent(type);
        }
        return null;
    }

    public String getTranslatedString(String key, Object... parameters) {
        return MessageFormat.format(getTranslatedString(key), parameters);
    }

    public static String getTranslatedString(String key, Class c, Object... parameters) {
        return MessageFormat.format(getTranslatedString(key, c), parameters);
    }

    public String getTranslatedString(String key) {
        return getTranslatedString(key, getClass());
    }
    private static ConcurrentHashMap<Class, Class> classMap = new ConcurrentHashMap();

    public static String getTranslatedString(String key, Class c) {
        Class clazz = classMap.get(c);
        if (clazz == null) {
            clazz = c;
            while (clazz.isAnonymousClass()) {
                clazz = clazz.getEnclosingClass();
            }
            classMap.put(c, clazz);
        }
        String classPrefix = c.getName();
        int dollarIndex = classPrefix.indexOf("$");
        if (dollarIndex != -1) {
            classPrefix = classPrefix.substring(0, dollarIndex);
        }
        return Localizer.getLocalizedString(classPrefix + "." + key);
    }

    public StringSource getTranslatedStringSource(final String key) {
        return new StringSource() {
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                return getTranslatedString(key);
            }
        };
    }

    public StringSource getTranslatedStringSource(final String key, final Object... parameters) {
        return new StringSource() {
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                return getTranslatedString(key, parameters);
            }
        };
    }

    public void registerService(HamsterComponent comp) {
        if (serviceMap == null) {
            serviceMap = new HashMap<Class, HamsterComponent>();
        }
        serviceMap.put(comp.getClass(), comp);
        if (comp.reverseServiceRegistrations == null) {
            comp.reverseServiceRegistrations = new ArrayList<HamsterComponent>();
        }
        comp.reverseServiceRegistrations.add(this);
    }

    public String getId() {
        if (id == -1) {
            id = super.hashCode();
        }
        return "" + id;
    }

    public HamsterAnimation getAnimation() {
        return animation;
    }

    public void setAnimation(HamsterAnimation animation) {
        this.animation = animation;
    }

    public Action getLastStaticAction() {
        return lastStaticAction;
    }

    public static String encode(String s) {
        return getStrictFilteredString(s);
    }

    public static String decode(String s) {
     return HTMLCodeFilter.decode(s);
    }

    public void printPath() {
        String path = "";
        HamsterComponent p = this;
        while (p != null) {
            path = p.getClass().getName() + "/" + path;
            if ((p.getParent() != null) && !p.getParent().components.contains(p)) {
            }
            p = p.getParent();
        }
        LOG.log(Level.INFO, "printPath: {0}", path);
    }

    private static final java.util.logging.Logger LOG = getLogger(HamsterComponent.class.getName());
}
