package com.kaibla.hamster.components.container;

import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.components.pageswitch.PageSwitch;
import com.kaibla.hamster.util.Template;
import java.util.LinkedList;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class Container extends HamsterComponent {

    private Template template;
    private HamsterComponent comp;
    private PageSwitch lastPS;

    public Container() {
        template = CONTAINER;
    }

    public Container(HamsterPage page) {
        super(page);
        template = CONTAINER;
    }

    public Container setComponent(HamsterComponent comp) {
        if (lastPS != null) {
            lastPS.setUnactive();
        }
        this.comp = comp;
        if (comp != null) {
            addComponent(comp);
        }
        //LOG.info("Container setComponent");
        markForUpdate();
        return this;
    }

    public HamsterComponent getComponent() {
        return comp;
    }

    public Container setComponent(HamsterComponent comp, PageSwitch ps) {
        if (lastPS != null) {
            lastPS.setUnactive();
        }
        this.comp = comp;
        addComponent(comp);
        lastPS = ps;
        markForUpdate();
        return this;
    }

//	@Override
//	public void remove(HamsterComponent comp) {
//		super.remove(comp);
//		if(comp == this.comp) {
//			setComponent(null);
//		}
//	}
//
//	@Override
//	public void removeAndDestroy(HamsterComponent comp) {
//		super.removeAndDestroy(comp);
//		if(comp == this.comp) {
//			setComponent(null);
//		}
//	}
//
//	@Override
//	public void removeAndDestroyAll() {
//		super.removeAndDestroyAll();
//		setComponent(null);
//	}
    public Container setTemplate(Template template) {
        this.template = template;
        markForUpdate();
        return this;
    }

    @Override
    public void generateHTMLCode() {
        LinkedList slots = new LinkedList();
        slots.add("" + getId());
        if (comp != null) {
            slots.add(comp);
        } else {
            slots.add("");
        }
        htmlCode = template.mergeStrings(slots, getPage());
    }
    private static transient Template CONTAINER = new Template(Container.class.getResource("container.html"));
    private static final Logger LOG = getLogger(Container.class.getName());
}
