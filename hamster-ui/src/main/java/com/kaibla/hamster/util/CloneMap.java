/*
 * CloneMap.java Created on 22. Februar 2007, 18:09
 */
package com.kaibla.hamster.util;

import com.kaibla.hamster.base.Action;
import com.kaibla.hamster.base.DataModel;
import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.UIEngine;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * @author Kai Orend
 */
public class CloneMap {

    private HashMap clones = null;

    /**
     * Creates a new instance of CloneMap
     */
    public CloneMap() {
        clones = new HashMap(100);
    }

    private Object getHamsterClone(Object orig) {
        if (orig == null) {
            return null;
        }
        Object k = clones.get(orig);
        if (k != null) {
            return k;
        } else {
            k = autoClone(orig);
            clones.put(orig, k);
            return k;
        }
    }

    public ArrayList cloneList(ArrayList list) {
        ArrayList result = new ArrayList(list.size());
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            result.add(getClone(iter.next()));
        }
        return result;
    }

    public HashMap getMap() {
        return clones;
    }
    
    

    public LinkedList cloneList(LinkedList list) {
        LinkedList result = new LinkedList();
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            result.add(getClone(iter.next()));
        }
        return result;
    }

    public void put(Object orig, Object clone) {
        clones.put(orig, clone);
    }

    public Object autoClone(Object obj) {
        Class c = obj.getClass();
        //LOG.info("Cloning: Class: " + c.getName());
        try {
            Object clone = c.newInstance();
            if (obj instanceof HamsterCloneable) {
                this.put(obj, clone);
            }
            if (obj instanceof HamsterComponent) {
                HamsterComponent comp = (HamsterComponent) clone;
                HamsterComponent orig = (HamsterComponent) obj;
            }
            while (c != null) {
                Field fields[] = c.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        Object value = field.get(obj);
                        value = getClone(value);
                        field.set(clone, value);
                    // LOG.info("Cloning: Class: "+c.getName()+"
                        // field: "+fields[i].getName());
                        field.setAccessible(false);
                    }
                }
                c = c.getSuperclass();
            }
            if (obj instanceof HamsterComponent) {
                HamsterComponent comp = (HamsterComponent) clone;
                HamsterComponent orig = (HamsterComponent) obj;
                comp.copyFrom(orig, this);
                comp.clearBuffer();
                orig.getPage().getEngine().addComponent(comp);
            }
            return clone;
        } catch (InstantiationException ex) {
            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
        } catch (IllegalAccessException ex) {
            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
        } catch (SecurityException ex) {
            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    public Object getClone(Object obj) {
        if (obj instanceof HamsterCloneable) {
            return getHamsterClone(obj);
        } else if (obj instanceof Collection) {
            return cloneCollection((Collection) obj);
        } else if (obj instanceof UIEngine || obj instanceof DataModel || obj instanceof Action) {
            return obj;
        }
        if (obj != null) {
            //LOG.info("CloneMap: Object is not cloneable: " + obj.getClass().getName());
        }
        return obj;
    }

    private Collection cloneCollection(Collection c) {
        try {
            Collection nc = c.getClass().newInstance();
            Iterator iter = c.iterator();
            while (iter.hasNext()) {
                nc.add(getClone(iter.next()));
            }
            return nc;
        } catch (InstantiationException ex) {
            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
        } catch (IllegalAccessException ex) {
            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }
    private static final Logger LOG = getLogger(CloneMap.class.getName());

}
