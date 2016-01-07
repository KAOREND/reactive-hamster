package com.kaibla.hamster.components.list;

import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterPage;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author kai
 */
public class MultiComponentEntry extends HamsterComponent {
    private static final long serialVersionUID = 1L;

    java.util.List<HamsterComponent> elements=new ArrayList();
    
    public MultiComponentEntry() {
    }

    public MultiComponentEntry(HamsterPage page) {
        super(page);
    }

    public List<HamsterComponent> getElements() {
        return elements;
    }
    
    public void add(HamsterComponent comp) {
        elements.add(comp);
    }
    
    
}
