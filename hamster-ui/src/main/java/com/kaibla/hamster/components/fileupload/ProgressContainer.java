/*
 * Registered.java
 *
 * Created on 15. August 2007, 18:47
 */
package com.kaibla.hamster.components.fileupload;

import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterPage;
import java.util.Iterator;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class ProgressContainer extends HamsterComponent {

    public ProgressContainer(HamsterPage page) {
        super(page);
    }

    /**
     * Creates a new instance of Registered
     */
    public ProgressContainer() {
    }

    @Override
    public void generateHTMLCode() {
        String s = "<div id=\"" + getId() + "\">";
        Iterator iter = components.iterator();
        while (iter.hasNext()) {
            s += "" + iter.next();
        }
        s += "</div>";
        htmlCode = s;
    }
    private static final Logger LOG = getLogger(ProgressContainer.class.getName());
}
