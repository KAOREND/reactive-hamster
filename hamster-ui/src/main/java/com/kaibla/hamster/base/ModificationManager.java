/*
 * ModificationManager.java Created on 23. Februar 2007, 11:08
 */
package com.kaibla.hamster.base;

import com.kaibla.hamster.base.Resumable;
import static com.kaibla.hamster.base.UIContext.setPage;
import static com.kaibla.hamster.base.HamsterComponent.encode;
import com.kaibla.hamster.util.Template;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import static javax.xml.parsers.SAXParserFactory.newInstance;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Kai Orend
 */
public class ModificationManager implements Serializable, Resumable {

    int n = 0;
    HashSet<HamsterComponent> mods = null;
    HashSet<HamsterComponent> renderedComponents = null;
    HashSet<HamsterComponent> animated = null;
    HashSet<HamsterComponent> appends = null;
    HashSet<HamsterComponent> notConfirmed = null;
    HashSet<HamsterComponent> notConfirmedAppends = null;
    HashSet<HamsterComponent> hidden = null;
    HashSet<HamsterComponent> hiddenAppends = null;
    LinkedHashMap<String, Script> scripts = new LinkedHashMap<String, Script>();
    transient HashSet<HamsterComponent> currentResult = new HashSet<>();
    LinkedList templates = new LinkedList();
    int scriptCount = 0;
    int count = 0;
    HamsterPage page = null;
    boolean working;
    boolean staticRequest = false;
    final transient static ThreadLocal<Boolean> visibilityChecking = new ThreadLocal<Boolean>();
//    private int debugCount=0;

    /**
     * Creates a new instance of ModificationManager
     */
    public ModificationManager(HamsterPage page) {
        mods = new HashSet<HamsterComponent>();
        notConfirmed = new HashSet<HamsterComponent>();
        animated = new HashSet<HamsterComponent>();
        appends = new HashSet<HamsterComponent>();
        notConfirmedAppends = new HashSet<HamsterComponent>();
        renderedComponents = new HashSet<HamsterComponent>();
        hidden = new HashSet<HamsterComponent>();
        hiddenAppends = new HashSet<HamsterComponent>();
        this.page = page;
        working = false;
        visibilityChecking.set(false);
    }

    public boolean isStaticRequest() {
        return staticRequest;
    }

    public void setStaticRequest(boolean staticRequest) {
        this.staticRequest = staticRequest;
    }

    public synchronized boolean isEmpty() {
        return mods.isEmpty() && animated.isEmpty() && appends.isEmpty() && scripts.
                isEmpty();
    }

    public boolean isWorking() {
        return working;
    }

    public synchronized boolean hasUnconfirmedChanges() {
        return !notConfirmed.isEmpty() || !notConfirmedAppends.isEmpty();
    }

    public void addRenderedComponent(HamsterComponent comp) {
        if (visibilityChecking.get() != null && visibilityChecking.get()) {
            renderedComponents.add(comp);
        }
        markAsContainedInCurrentModificationXML(comp);
    }

    public static void cleanUpThreadLocal() {
        if (visibilityChecking == null) {
            visibilityChecking.remove();
        }
    }

    public boolean isAlreadyInCurrentResult(HamsterComponent comp) {
        return currentResult.contains(comp);
    }

    public void markAsContainedInCurrentModificationXML(HamsterComponent comp) {
        currentResult.add(comp);
    }

    public synchronized String getModificiationXML() {
        page.getLock().lock();
        try {
            page.onPageUpdate();
            currentResult.clear();

//            LOG.info("getModificationXML "+debugCount+"  page: "+page.getId());
//            debugCount++;
            setPage(page);
            page.markAsAlive();
            working = true;
            prepare();
            page.fireOnShow();

            if (isEmpty()) {
                working = false;
                return "n";
            }
            LinkedList params = new LinkedList();
            params.add(new Integer(count++));
            StringBuilder buf = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            buf.append("<root cu=\"").append(page.getCLink(params)).append("\">");

            mods.addAll(animated);
            animated.clear();
            HashSet<HamsterComponent> oldMods = mods;

            notConfirmed.addAll(mods);
            mods = new HashSet<HamsterComponent>();
            currentResult.clear();
            for (HamsterComponent comp : oldMods) {
                if (!isAlreadyInCurrentResult(comp)) {
                    buf.append("<replace id=\"").append(comp.getId()).append("\">");
                    if ((comp.getAnimation() != null) && comp.
                            isMarkedForAnimation()) {
                        buf.append(comp.getAnimation().getXML());
                    }
                    buf.append("<r>");
                    String replacement = comp.getHTMLCode().replaceAll("<br>", "<br></br>");
                    replacement = replacement.trim();
                    buf.append(replacement);
                    buf.append("</r>");
                    buf.append("</replace>");
                }
            }
            HashSet<HamsterComponent> oldAppends = appends;
            notConfirmedAppends.addAll(appends);
            appends = new HashSet<HamsterComponent>();
            for (HamsterComponent comp : oldAppends) {
                HamsterComponent parent = comp.getParent();
                int index = parent.components.indexOf(comp);
                HamsterComponent previous = parent.components.get(index - 1);
                buf.append("<append id=\"").append(previous.getId()).
                        append("\">");
                if ((comp.getAnimation() != null) && comp.
                        isMarkedForAnimation()) {
                    buf.append(comp.getAnimation().getXML());

                }
                buf.append("<r>");
                buf.append(comp.getHTMLCode().
                        replaceAll("<br>", "<br></br>"));
                buf.append("</r>");
                buf.append("</append>");
            }

            for (Script s : scripts.values()) {
                buf.append("<script>");
                buf.append(encode(s.s));
                buf.append("</script>");
            }
            scripts.clear();

            Iterator iter = templates.iterator();
            while (iter.hasNext()) {
                buf.append(((Template) iter.next()).getTemplate(page));
            }
            templates.clear();

            buf.append("</root>");
            try {
                SAXParser saxParser = newInstance().
                        newSAXParser();
                saxParser.getXMLReader().
                        parse(new InputSource((new StringReader(buf.
                                                toString()))));

            } catch (ParserConfigurationException ex) {
                LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);

                LOG.log(Level.SEVERE, "Error in MOD_XML:{0}", buf.toString());

            } catch (SAXException ex) {
                LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);

                LOG.log(Level.SEVERE, "Error in MOD_XML:{0}", buf.toString());
            } //		 LOG.info("===================================================================>>> MOD_XML:");
            //		 LOG.info(buf);
            catch (IOException ex) {
                LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);

                LOG.log(Level.SEVERE, "Error in MOD_XML:{0}", buf.toString());
            }
//		 LOG.info("===================================================================>>> MOD_XML:");
//		 LOG.info(buf);

//		 LOG.info("===================================================================>>> MOD_XML:");
//		 LOG.info(buf);
            working = false;
            staticRequest = false;
            return buf.toString();
        } finally {
            page.getLock().unlock();
        }
    }

    public synchronized void addTemplate(Template t) {
        if (!templates.contains(t)) {
            templates.add(t);
        }

    }

    public synchronized void addMod(HamsterComponent comp) {
//        LOG.info("addMod "+debugCount+"  page: "+page.getId());
        if (!(comp instanceof HamsterPage)) {
            if ((comp.getAnimation() != null) && comp.isMarkedForAnimation() && !animated.
                    contains(comp)) {
                animated.add(comp);
            } else if (!mods.contains(comp)) {
                mods.add(comp);
            }
        }
        page.getListenerContainer().interrupt();
    }

    public synchronized void removeComponent(HamsterComponent comp) {
        mods.remove(comp);
        animated.remove(comp);
        appends.remove(comp);
        for (HamsterComponent child : comp.components) {
            removeComponent(child);
        }
    }

    public synchronized void appendChild(HamsterComponent comp) {
//        LOG.info("appendChild "+debugCount+"  page: "+page.getId());     
        if (comp.parent == null) {
            throw new IllegalArgumentException("components parent must be set for append");
        }
        appends.add(comp);
        page.getListenerContainer().interrupt();
    }

    private void prepare() {
        for (HamsterComponent appendedComponent : notConfirmedAppends) {
            //we cannot send appends twice, so we have to fall back
            //to update the parent
            if (appendedComponent.getParent() != null) {
                appendedComponent.getParent().markForUpdate();
            }
        }
        for (HamsterComponent appendedComponent : hiddenAppends) {
            //we cannot send appends twice, so we have to fall back
            //to update the parent
            if (appendedComponent.getParent() != null) {
                appendedComponent.getParent().markForUpdate();
            }
        }
        mods.addAll(notConfirmed);
        //mods.addAll(hidden);
        mods.removeAll(appends);
        notConfirmed.clear();
        notConfirmedAppends.clear();
        calculateVisibilites();
        for (HamsterComponent comp : hidden) {
            if (comp.isVisible()) {
                mods.add(comp);
            }
        }
        optimize();
    }

    private void calculateVisibilites() {
        optimizeAndRender(mods);
        optimizeAndRender(appends);
    }

    private void optimizeAndRender(HashSet<HamsterComponent> result) {

        Iterator<HamsterComponent> iter = result.iterator();
        while (iter.hasNext()) {
            HamsterComponent comp = iter.next();
            if (!(comp instanceof HamsterPage) && (comp.getParent() == null || !(comp.getParent().components.contains(comp)))) {
                //that one does not exist anymore
                iter.remove();
            }
        }
        HashSet<HamsterComponent> components = new HashSet(result);
        while (!components.isEmpty()) {
            optimizeAndRender(components, result, components.iterator().next());
        }
    }

    private boolean optimizeAndRender(HashSet<HamsterComponent> components, HashSet<HamsterComponent> result, HamsterComponent comp) {
        if (comp == null) {
            return true;
        }
        if (comp instanceof HamsterPage) {
            renderedComponents.clear();
            visibilityChecking.set(true);
            comp.getHTMLCode();
            visibilityChecking.set(false);

            if (components.contains(comp)) {
                if (result == mods) {
                    appends.removeAll(renderedComponents);
                }
                result.add(comp);
            }
            components.remove(comp);
            components.removeAll(renderedComponents);
            renderedComponents.clear();
            return true;
        }
        if (!components.contains(comp)) {
            return optimizeAndRender(components, result, comp.getParent()) && comp.isVisible();
        }
        if (optimizeAndRender(components, result, comp.getParent()) && comp.isVisible()) {
            renderedComponents.clear();
            visibilityChecking.set(true);
            comp.getHTMLCode();
            visibilityChecking.set(false);
            result.removeAll(renderedComponents);
            if (result == mods) {
                appends.removeAll(renderedComponents);
            }
            result.add(comp);
            components.removeAll(renderedComponents);
            components.remove(comp);
            renderedComponents.clear();
            return true;
        } else {
            components.remove(comp);
            result.remove(comp);
            if (result == mods) {
                hidden.add(comp);
            } else if (result == appends) {
                hiddenAppends.add(comp);
            }

            return false;
        }
    }

    /*
     * Optimize after following rule: If the parent of a component is rendered, the child can be removed
     * from the mod list
     */
    private void optimize() {
        removeUnneeded(mods);
        removeUnneeded(appends);
        hidden.removeAll(mods);
        hiddenAppends.removeAll(appends);
    }

    private boolean isInResult(HamsterComponent comp) {
        return animated.contains(comp) || mods.contains(comp) || appends.contains(comp);
    }

    private void removeUnneeded(HashSet<HamsterComponent> source) {
        Iterator<HamsterComponent> iter = source.iterator();
        boolean hasChanged = true;
        while (hasChanged) {
            hasChanged = false;
            while (iter.hasNext()) {
                HamsterComponent comp = iter.next();
                HamsterComponent parent = comp.parent;
                while (parent != comp && parent != null && isInResult(parent) && !hasChanged) {
                    iter.remove();
                    hasChanged = true;
                    parent = parent.parent;
                }
            }
        }
    }

    public int getConfirmationCounter() {
        return count;
    }

    public synchronized void confirmLastModificationXML(int id) {
//         LOG.info("confirmLastModification: count: "+count+"confirmed: "+(id+2));
        if (count <= (id + 2)) {
            for (HamsterComponent comp : notConfirmed) {
                comp.confirmCache();
                comp.unmarkForAnimation();
            }
            for (HamsterComponent comp : notConfirmedAppends) {
                comp.confirmCache();
            }
            notConfirmed.clear();
            notConfirmedAppends.clear();
            if (!isEmpty()) {
                page.getEngine().updateSinglePage(page.getListenerContainer());
            }
        }
    }

    /**
     * Executes the given javascript in the browser after the next refresh.
     */
    public synchronized void executeScriptOnClient(String script) {
        if (!scripts.containsKey(script)) {
            Script s = new Script();
            scriptCount++;
            s.id = scriptCount;
            s.s = script;
            scripts.put(script, s);
        }
    }

    /**
     * Executes the given javascript in the browser after the next refresh.
     */
    public synchronized void executeScriptOnClientFirst(String script) {
        if (!scripts.containsKey(script)) {
            Script s = new Script();
            scriptCount++;
            s.id = scriptCount;
            s.s = script;
            LinkedHashMap<String, Script> old = scripts;
            scripts = new LinkedHashMap<String, Script>();
            scripts.put(script, s);
            scripts.putAll(old);
        }
    }

    public synchronized void removeScript(String script) {
        scripts.remove(script);
    }

    public synchronized String getAllScripts() {
        StringBuilder builder = new StringBuilder();
        for (Script script : scripts.values()) {
            builder.append(script.s).append(";\n");
        }
        return builder.toString();
    }

    public synchronized void reset() {
        mods.clear();
        appends.clear();
        //scripts.clear();
        notConfirmed.clear();
        notConfirmedAppends.clear();
        //scriptCount = 0;
        staticRequest = false;
        hidden.clear();
        hiddenAppends.clear();
    }

    public void clearScripts() {
        scripts.clear();
    }

    @Override
    public void resume() {
       currentResult = new HashSet<HamsterComponent>();
    }

    private class Script implements Serializable {

        int id = 0;
        String s;
    }
    private static final Logger LOG = getLogger(ModificationManager.class.getName());
}
