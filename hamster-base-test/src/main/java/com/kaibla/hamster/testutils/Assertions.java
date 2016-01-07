/*
 * 
 * .
 */
package com.kaibla.hamster.testutils;

import com.kaibla.hamster.persistence.model.Document;
import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author kai
 */
public class Assertions {

    public static void assertOrder(Collection l1, Document... mo) {
        assertOrder(l1, asList(mo));
    }

    public static void assertOrder(Collection l1, Collection l2) {
        Iterator iter1 = l1.iterator();
        Iterator iter2 = l2.iterator();
        while (iter1.hasNext()) {
           Object mo1 =  iter1.next();
            assertTrue("Lists should have the same size " + l1.size() + "  " + l2.
                    size(), iter2.hasNext());
            Object mo2 =  iter2.next();
            assertTrue("order is not the same", mo1 == mo2);
        }
        assertTrue("Lists should have the same size", iter2.hasNext() == iter1.
                hasNext());
    }
    private static final Logger LOG = getLogger(Assertions.class.getName());
}
