package com.kaibla.hamster.components.list;

import com.kaibla.hamster.base.Action;
import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.DataModel;
import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.base.Resumable;
import com.kaibla.hamster.persistence.events.DataObjectCreatedEvent;
import com.kaibla.hamster.persistence.events.DataObjectDeletedEvent;
import com.kaibla.hamster.persistence.events.ListChangedEvent;
import com.kaibla.hamster.persistence.model.ListModel;
import com.kaibla.hamster.persistence.model.DatabaseListModel;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.util.Template;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public abstract class List extends HamsterComponent implements Resumable {

    private DatabaseListModel model;
    private Template template;
    private int elementsPerPage = -1;
    private transient HashMap<DataModel, HamsterComponent> cache = new HashMap<DataModel, HamsterComponent>();
    private transient HashMap<HamsterComponent, DataModel> reverseCache = new HashMap<HamsterComponent, DataModel>();
    transient LinkedList currentComponents = new LinkedList();
    LinkedList pageLists = new LinkedList();
    private long currentPage = 0;
    private long elements = 0;
    private boolean hasMoreElements;
    private final int countStep = 50;
    transient SortedSet data;
    private String name;
    private boolean onMouseOver;
    private String staticParent;
    private String separator;
    private boolean replaceOnUpdate = false;
    private int currentElementIndex = 0;
    private int elementsOnPage = 0;
    private boolean reverse = false;
    private boolean endless = false;
    private boolean endlessPageScroll = false;
    private HamsterComponent lastElement = null;

    public List() {
    }

    public List(HamsterPage page, String name, DatabaseListModel model, int elementsPerPage, boolean reverse, boolean endless) {
        super(page);
        this.name = name;
        this.elementsPerPage = elementsPerPage;
        if (endless) {
            template = LIST_SCROLLABLE;
        } else {
            template = LIST;
        }
        this.reverse = reverse;
        this.endless = endless;
        this.model = model;
    }

    public void setEndlessPageScroll(boolean endlessPageScroll) {
        this.endlessPageScroll = endlessPageScroll;
    }

    public List(HamsterPage page, String name, DatabaseListModel model, int elementsPerPage) {
        super(page);
        this.name = name;
        this.elementsPerPage = elementsPerPage;
        template = LIST;
        this.model = model;
    }

    @Override
    public void init() {
        super.init(); //To change body of generated methods, choose Tools | Templates.
        setModel(model);
        setModel(model);
        if (endless) {
            loadNextBlock();
        }
    }

    public List(HamsterPage page, String name, DatabaseListModel model) {
        super(page);
        this.name = name;
        template = LIST;
        setModel(model);
    }

    public void setElementsPerPage(int elementsPerPage) {
        this.elementsPerPage = elementsPerPage;
    }
    
    

    public void setLastElement(HamsterComponent lastElement) {
        this.lastElement = lastElement;
        addComponent(lastElement);
        markForUpdate();
    }

    public DatabaseListModel getModel() {
        return model;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public void setEndless(boolean endless) {
        this.endless = endless;
    }

    @Override
    public void markForUpdate() {
        super.markForUpdate();
        Iterator iter = pageLists.iterator();
        while (iter.hasNext()) {
            PageList p = (PageList) iter.next();
            p.markForUpdate();
        }
    }

    @Override
    public void updateAll() {
        gotoPage(currentPage);
        super.updateAll();
    }

    public SortedSet getCurrentlyShownData() {
        return data;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getSeparator() {
        return separator;
    }

    public String getName() {
        return name;
    }

    public void addPageList(PageList l) {
        pageLists.add(l);
    }

    public int getPageCount() {
        return (int) Math.ceil((double) elements / (double) elementsPerPage);
    }

    public long getCurrentPage() {
        return currentPage;
    }

    public void onPageCountIncreased() {
    }

    public void gotoPage(long page) {
        if (!endless) {
            while (hasMoreElements && page > getPageCount() - 5) {
                nextCountStep();
            }
        }
        if (endless) {
            showData(model.get(0, elements));
        } else if (elementsPerPage == -1) {
            showData(model.get());
        } else {
            showData(model.get(page * elementsPerPage, elementsPerPage));
        }
        currentPage = page;
        markForUpdate();
    }

    /**
     * Load next Block for endless scrolling
     */
    public void loadNextBlock() {
        int oldCount = (int) elements;
        elements = model.
                getSize(oldCount + elementsPerPage);
        if (elements > oldCount) {
            setHasMoreElements(true);
            elements = oldCount + elementsPerPage;
            showData(model.get(0, elements));
        } else {
            hasMoreElements = false;
        }
        exec("hamster.scrolling.resetEndlessScroll('" + getId() + "')");
    }

    public class LoadNextBlockAction extends Action {

        public LoadNextBlockAction() {
        }

        @Override
        public void invoke() {
            loadNextBlock();
        }
    }

    public LoadNextBlockAction getLoadNextBlockAction() {
        return new LoadNextBlockAction();
    }

    private void showData(SortedSet data) {
        this.data = data;
        elementsOnPage = data.size();
        currentElementIndex = 0;
        synchronized (currentComponents) {

            HashSet<HamsterComponent> toRemove = new HashSet();
            HashSet<HamsterComponent> added = new HashSet();
            added.addAll(components);
            toRemove.addAll(currentComponents);
            currentComponents.clear();
            Iterator iter = data.iterator();
            while (iter.hasNext()) {
                DataModel m = (DataModel) iter.next();
                m.addHolder(this);
                HamsterComponent c = getComponent(m);
                toRemove.remove(c);
                if (c != null) {
                    if (c instanceof MultiComponentEntry) {
                        for (HamsterComponent e : ((MultiComponentEntry) c).getElements()) {
                            if (!added.contains(e)) {
                                addComponent(e);
                            }
//                    c.markForUpdate();
                            if (reverse) {
                                currentComponents.addFirst(e);
                            } else {
                                currentComponents.add(e);
                            }
                        }
                    } else {
                        if (!added.contains(c)) {
                            addComponent(c);
                        }
//                    c.markForUpdate();
                        if (reverse) {
                            currentComponents.addFirst(c);
                        } else {
                            currentComponents.add(c);
                        }
                    }

                }
                currentElementIndex++;
            }
            iter = toRemove.iterator();
            while (iter.hasNext()) {
                HamsterComponent c = (HamsterComponent) iter.next();
                removeFromCache(c);
            }
        }
        markForUpdate();
    }

    private HamsterComponent getComponent(DataModel m) {
        HamsterComponent c = cache.get(m);
        if (c == null) {
            c = renderElement(m);
            acquireDataModel(m);
            addToCache(m, c);
        }
        return c;
    }

    private void addToCache(DataModel m, HamsterComponent c) {
        cache.put(m, c);
        reverseCache.put(c, m);
    }

    private void removeFromCache(DataModel m) {
        HamsterComponent c = cache.remove(m);
        if (c != null) {
            reverseCache.remove(c);
            if (c instanceof MultiComponentEntry) {
                for (HamsterComponent e : ((MultiComponentEntry) c).getElements()) {
                    removeAndDestroy(e);
                    currentComponents.remove(e);
                }
            } else {
                removeAndDestroy(c);
                currentComponents.remove(c);
            }
        }
    }

    private void removeFromCache(HamsterComponent c) {
        DataModel m = reverseCache.remove(c);
        if (m != null) {
            cache.remove(m);
        }
    }

    public void reset() {       
        for(DataModel m: new ArrayList<DataModel>(reverseCache.values())) {
            removeFromCache(m);
        }
        reverseCache.clear();
        cache.clear();
        hasMoreElements=true;
        currentPage=0; 
        elements=elementsPerPage;
    }
    private void addEntry(HamsterComponent c) {
        if (c instanceof MultiComponentEntry) {
            for (HamsterComponent e : ((MultiComponentEntry) c).getElements()) {
                if (lastElement == null) {
                    appendComponent(e);
                } else {
                    addComponent(e);
                    markForUpdate();
                }
            }
        } else {
            if (lastElement == null) {
                appendComponent(c);
            } else {
                addComponent(c);
                markForUpdate();
            }
        }

        currentComponents.add(c);
    }

    @Override
    public void generateHTMLCode() {
        LinkedList slots = new LinkedList();
        slots.add(getId());
        StringBuilder s = new StringBuilder();

        Iterator iter = currentComponents.iterator();

        while (iter.hasNext()) {
            s.append(iter.next());
            if (separator != null && iter.hasNext()) {
                s.append(separator);
            }
        }
        if (lastElement != null) {
            s.append(lastElement);
        }
        slots.add(s);
        htmlCode = template.mergeStrings(slots, getPage());

        if (endless && hasMoreElements) {
            if (endlessPageScroll) {
                exec("hamster.scrolling.setEndlessPageScroll('"
                        + getActionURL(getLoadNextBlockAction())
                        + "','"
                        + getId()
                        + "'," + reverse + ");");
            } else {
                exec("hamster.scrolling.setEndlessScroll('"
                        + getActionURL(getLoadNextBlockAction())
                        + "','"
                        + getId()
                        + "'," + reverse + ");");
            }
        }
        if (endless) {
            exec("hamster.scrolling.resetEndlessScroll('" + getId() + "')");
        }
    }
    private static transient Template LIST = new Template(List.class.
            getResource("list.html"));

    private static transient Template LIST_SCROLLABLE = new Template(List.class.
            getResource("list_scrollable.html"));

    public abstract HamsterComponent renderElement(DataModel data);

    /**
     * @param model the model to set
     */
    public void setModel(DatabaseListModel model) {
        if (this.model != null) {
            this.model.removeChangedListener(this);
        }       
        this.model = model;
        if (!endless) {
            nextCountStep();
        }
        model.addChangedListener(this);
        gotoPage(currentPage);
    }

    private void nextCountStep() {
        if (elementsPerPage > 0) {
            int oldCount = (int) elements;
            elements = model.
                    getSize(oldCount + elementsPerPage * (countStep + 1));
            if (elements > oldCount + elementsPerPage * countStep) {
                setHasMoreElements(true);
                elements = oldCount + elementsPerPage * countStep;
            } else {
                hasMoreElements = false;
            }
        }
    }

    /**
     * @param template the template to set
     */
    public void setTemplate(Template template) {
        this.template = template;
    }

    /**
     * @return the onMouseOver
     */
    public boolean isOnMouseOver() {
        return onMouseOver;
    }

    /**
     * @param onMouseOver the onMouseOver to set
     */
    public void setOnMouseOver(boolean onMouseOver) {
        this.onMouseOver = onMouseOver;
    }

    /**
     * @return the staticParent
     */
    public String getStaticParent() {
        return staticParent;
    }

    /**
     * @param staticParent the staticParent to set
     */
    public void setStaticParent(String staticParent) {
        this.staticParent = staticParent;
    }

    @Override
    public void dataChanged(DataEvent e) {
//        LOG.log(Level.INFO, "List onDatachanged event {0}", e);
        //super.dataChanged(e);

        if (e instanceof DataObjectCreatedEvent) {
            DataObjectCreatedEvent ec = (DataObjectCreatedEvent) e;
            Document o = ec.getMongoObject();
            if (model.contains(o)) {
                elements++;
                if (replaceOnUpdate) {
                    DataModel m = e.getSource();
                    removeFromCache(m);
//                    markForUpdate();
                    gotoPage(currentPage);
                } else if (!cache.containsKey(e.getSource())) {
                    //only update if this element was not already inserted
                    //special case optimization: if something is appended to the list:
                    if ((model.get().last() == e.getSource() && !reverse) || (model.get().first() == e.getSource() && reverse)) {
                        addEntry(getComponent(e.getSource()));
                    } else {
                        gotoPage(currentPage);
                    }
                }
            } else {
            }
//            }
        } else if (e instanceof DataObjectDeletedEvent) {
            DataObjectDeletedEvent de = (DataObjectDeletedEvent) e;
            DataModel m = de.getMongoObject();
            if (cache.containsKey(m)) {
                HamsterComponent c = cache.get(m);
                removeAndDestroy(c);
                removeFromCache(m);
                gotoPage(currentPage);
            }
        } else if (e instanceof ListChangedEvent) {
            gotoPage(currentPage);
        } else {

            if (replaceOnUpdate) {
                DataModel m = e.getSource();
                removeFromCache(m);
//                markForUpdate();
            }
            gotoPage(currentPage);
        }
    }

    public void setReplaceOnUpdate(boolean replaceOnUpdate) {
        this.replaceOnUpdate = replaceOnUpdate;
    }

    public void reRender(DataModel m) {
        removeFromCache(m);
        markForUpdate();
    }

    /**
     * @return the hasMoreElements
     */
    public boolean isHasMoreElements() {
        return hasMoreElements;
    }

    /**
     * @param hasMoreElements the hasMoreElements to set
     */
    public void setHasMoreElements(boolean hasMoreElements) {
        this.hasMoreElements = hasMoreElements;
    }

    /**
     * @return the currentElementIndex
     */
    protected int getCurrentElementIndex() {
        return currentElementIndex;
    }

    /**
     * @param currentElementIndex the currentElementIndex to set
     */
    private void setCurrentElementIndex(int currentElementIndex) {
        this.currentElementIndex = currentElementIndex;
    }

    /**
     * @return the elementsOnPage
     */
    protected int getElementsOnPage() {
        return elementsOnPage;
    }

    /**
     * @param elementsOnPage the elementsOnPage to set
     */
    private void setElementsOnPage(int elementsOnPage) {
        this.elementsOnPage = elementsOnPage;
    }

    protected Object writeReplace() {
        removeAndDestroyAll();
        return this;
    }

    @Override
    public void resume() {
        super.afterResume();
        cache = new HashMap<DataModel, HamsterComponent>();
        reverseCache = new HashMap<HamsterComponent, DataModel>();
        currentComponents = new LinkedList();
        setModel(model);
    }

    private static final Logger LOG = getLogger(List.class.getName());
}
