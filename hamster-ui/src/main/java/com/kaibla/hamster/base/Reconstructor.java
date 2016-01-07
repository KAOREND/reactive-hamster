/*
 * Reconstructor.java Created on 8. August 2007, 19:37
 */
package com.kaibla.hamster.base;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * @author Kai Orend
 */
public class Reconstructor {

    private final LinkedList list = new LinkedList();

    public Reconstructor() {

    }

    public boolean createRelativePath = true;

    /**
     * Creates a new instance of Reconstructor
     */
    public Reconstructor(String path, UIEngine eninge) {

        StringTokenizer t = new StringTokenizer(path, "?");
        while (t.hasMoreTokens()) {
            String token = t.nextToken();
            int i = token.indexOf('=');
            Element e = new Element();
            if (i == -1) {
                e.name = token;
            } else {
                e.name = token.substring(0, i);
                e.param = token.substring(i + 1);
            }
            list.add(e);
        }
    }

    public Reconstructor(LinkedList path, UIEngine eninge) {
        Iterator iter = path.iterator();
        while (iter.hasNext()) {
            String token = "" + iter.next();
            int i = token.indexOf('=');
            Element e = new Element();
            if (i == -1) {
                e.name = token;
            } else {
                e.name = token.substring(0, i);
                e.param = token.substring(i + 1);
            }
            list.add(e);
        }
    }

    public LinkedList list() {
        return list;
    }

    public class Element {

        String name = "";
        String param = "";

        public String getName() {
            return name;
        }

        public String getParam() {
            return param;
        }
    }
    private static final Logger LOG = getLogger(Reconstructor.class.getName());

}
