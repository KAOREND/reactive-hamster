package com.kaibla.hamster.components.pageswitch;

import com.kaibla.hamster.base.ChangedListener;
import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.components.container.Container;
import java.io.Serializable;

/**
 *
 * @author kai
 */
public abstract class ComponentFactory implements ChangedListener, Serializable {

    private HamsterComponent comp = null;

    public abstract HamsterComponent createComponent();
    private final HamsterComponent owner;

    public ComponentFactory(HamsterComponent owner) {
        this.owner = owner;
    }

    @Override
    public boolean isDestroyed() {
        return owner.isDestroyed();
    }

    public void rebuild() {
        if (comp != null) {
            HamsterComponent old = comp;
            comp = null;
            if (old.parent != null) {
                if (old.parent instanceof Container) {
                    Container c = (Container) old.parent;
                    c.setComponent(comp);
                }
                old.parent.replaceComponent(old, getComponent());
            }
            old.destroy();
        }
    }

    public HamsterComponent getComponent() {
        if (comp == null) {
            comp = createComponent();
        }
        return comp;
    }

    @Override
    public void dataChanged(DataEvent e) {
        rebuild();
    }
}
