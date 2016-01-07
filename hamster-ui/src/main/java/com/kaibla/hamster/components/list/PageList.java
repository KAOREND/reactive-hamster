package com.kaibla.hamster.components.list;

import com.kaibla.hamster.base.Action;
import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.util.Template;
import static java.lang.Integer.parseInt;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Kai Orend
 */
public class PageList extends HamsterComponent {

    protected Template template;
    protected Template nextTemplate;
    protected Template prevTemplate;
    protected Template numberTemplate;
    protected Template activeNumberTemplate;
    protected Template placeholderTemplate;
    protected String numberCSS = "";
    protected String activeCSS = "";
    protected String nextCSS = "";
    protected String prevCSS = "";
    protected String cssClass = "";
    protected boolean onMouseOver = false;
    protected List list;
    protected static Template PAGELIST = new Template(PageList.class.getResource("pagelist.html"));
    protected static Template NUMBER = new Template(PageList.class.getResource("number.html"));
    protected static Template ACTIVENUMBER = new Template(PageList.class.
            getResource("activenumber.html"));

    public PageList() {
    }

    public PageList(HamsterPage page, List l) {
        super(page);
        list = l;
        l.addPageList(this);
        template = PAGELIST;
        activeNumberTemplate = ACTIVENUMBER;
        numberTemplate = NUMBER;
    }

    @Override
    public void generateHTMLCode() {
        LinkedList slots = new LinkedList();
        slots.add(getId());
        if (template == PAGELIST) {
            slots.add(cssClass);
        }
        LinkedList temp = new LinkedList();
        String s = "";
        int pages = list.getPageCount();

        for (int i = 0; i < pages; i++) {
            temp = new LinkedList();

            String p = "";
            if (list.getCurrentPage() == i) {
                temp.add("" + i);
                s += activeNumberTemplate.mergeStrings(temp, getPage());
            } else {
                if (numberCSS != null) {
                    p = numberCSS;
                }
                if (isOnMouseOver()) {
                    temp.add(getOnMouseOverActionLinkTag(new PageAction(i), p) + i + "</a>");
                    s += numberTemplate.mergeStrings(temp, getPage());
                } else {
                    temp.add(getActionLinkTag(new PageAction(i), p) + i + "</a>");
                    s += numberTemplate.mergeStrings(temp, getPage());
                }
            }
            LOG.log(Level.INFO, "page: {0}", i);
        }
        slots.add(s);
        htmlCode = template.mergeStrings(slots, getPage());
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

    public class PageAction extends Action {

        long page;
        long oldPage = 0;

        public PageAction(long page) {
            this.page = page;
        }

        public PageAction() {
        }

        @Override
        public void invoke() {
            oldPage = list.getCurrentPage();
            LOG.log(Level.INFO, "pageAction on list {0} goto page: {1}", new Object[]{list.getName(), page});

            list.gotoPage(page);
        }

        @Override
        public String getStaticName() {
            if (list != null) {
                return list.getName();
            } else {
                return null;
            }
        }

        @Override
        public String getStaticParent() {
            return list.getStaticParent();
        }

        @Override
        public String getStaticParameter() {
            return "" + page;
        }

        @Override
        public void setStaticParameter(String s) {
            page = parseInt(s);
        }
    }

    /**
     * @param template the template to set
     */
    public void setTemplate(Template template) {
        this.template = template;
    }

    /**
     * @param nextTemplate the nextTemplate to set
     */
    public void setNextTemplate(Template nextTemplate) {
        this.nextTemplate = nextTemplate;
    }

    /**
     * @param prevTemplate the prevTemplate to set
     */
    public void setPrevTemplate(Template prevTemplate) {
        this.prevTemplate = prevTemplate;
    }

    public void setActiveCSS(String activeCSS) {
        this.activeCSS = activeCSS;
    }

    public void setNumberCSS(String numberCSS) {
        this.numberCSS = numberCSS;
    }

    public void setNextCSS(String nextCSS) {
        this.nextCSS = nextCSS;
    }

    public void setPrevCSS(String prevCSS) {
        this.prevCSS = prevCSS;
    }

    /**
     * @param numberTemplate the numberTemplate to set
     */
    public void setNumberTemplate(Template numberTemplate) {
        this.numberTemplate = numberTemplate;
    }

    /**
     * @param activeNumberTemplate the activeNumberTemplate to set
     */
    public void setActiveNumberTemplate(Template activeNumberTemplate) {
        this.activeNumberTemplate = activeNumberTemplate;
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
    public PageList setOnMouseOver(boolean onMouseOver) {
        this.onMouseOver = onMouseOver;
        return this;
    }
    private static final Logger LOG = getLogger(PageList.class.getName());
}
