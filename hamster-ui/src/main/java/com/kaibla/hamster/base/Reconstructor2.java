/*
 * Reconstructor.java Created on 8. August 2007, 19:37
 */
package com.kaibla.hamster.base;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * @author Kai Orend
 */
public class Reconstructor2 extends Reconstructor {

    private final LinkedList list = new LinkedList();

    /**
     * Creates a new instance of Reconstructor
     */
    public Reconstructor2(String path, UIEngine eninge) {

        if (path.endsWith(".hsp")) {
            path = path.substring(0, path.length() - 4);
        }
        LOG.log(Level.FINEST, "Reconstruct2: {0}", path);
        StringTokenizer t = new StringTokenizer(path, "/");
        if (t.countTokens() % 2 == 0) {
            while (t.hasMoreTokens()) {
                String token = t.nextToken();
                Element e = new Element();
                e.name = token;
                e.param = t.nextToken();
                LOG.log(Level.INFO, "Reconstruct2: element {0} {1}", new Object[]{e.name, e.param});
                list.add(e);
            }
        }
    }

    public Reconstructor2(LinkedList path, UIEngine eninge) {
        Iterator iter = path.iterator();
        while (iter.hasNext()) {
            String token = "" + iter.next();
            Element e = new Element();
            e.name = token;
            e.param = "" + iter.next();
            list.add(e);
            LOG.log(Level.INFO, "Reconstruct2: element {0} {1}", new Object[]{e.name, e.param});
        }
    }

    @Override
    public LinkedList list() {
        return list;
    }
    private static final Logger LOG = getLogger(Reconstructor2.class.getName());
}
