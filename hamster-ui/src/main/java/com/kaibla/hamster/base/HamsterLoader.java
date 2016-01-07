/*
 * HamsterLoader.java Created on 7. August 2007, 17:32
 */
package com.kaibla.hamster.base;

import static com.kaibla.hamster.base.HamsterComponent.onClassLoad;
import static com.kaibla.hamster.base.UIEngine.addError;
import static com.kaibla.hamster.base.UIEngine.registerStaticAction;
import com.kaibla.hamster.util.Template;
import java.io.File;
import java.io.IOException;
import static java.lang.Class.forName;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isStatic;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 * @author Kai Orend
 */
public class HamsterLoader {

    HashSet loadedClasses = new HashSet();
    UIEngine engine = null;

  
    
    public HamsterLoader(UIEngine engine) {
        this.engine = engine;
    }

    public HamsterLoader() {

    }

    public void setEngine(UIEngine engine) {
        this.engine = engine;
    }

    private void loadTemplates(Class c) {
        LOG.info("loading Templates for HamsterComponent "+c.getName());
              try {
                // LOG.info("searching fields of " + comp.getClass().getCanonicalName());
                Field[] fields = c.getDeclaredFields();
                for (Field field : fields) {
                    // LOG.info("field: " + fields[i].getDeclaringClass().getCanonicalName() + "/"
                    // + fields[i].getName());
                    if (Template.class.isAssignableFrom(field.getType()) && isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        try {
                            ((Template) field.get(null)).load();
                        } catch (Exception ex) {
                            addError("Error while loading template " + field.getName() + " from " + c.
                                    getCanonicalName() + "  \n" + ex + "  ");
                            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
                        }
                        field.setAccessible(false);
                    }
                }

            } catch (SecurityException e) {
                addError("Field parsing failed in " + c.
                        getName());
                LOG.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            }
    }
    public void loadComponent(Class c) {
        if(loadedClasses.contains(c)) return;        
        loadedClasses.add(c);
        loadTemplates(c);
        if (HamsterComponent.class.isAssignableFrom(c) && !c.equals(HamsterComponent.class) && !c.getName().contains("$")) {
            LOG.info("loading HamsterComponent "+c.getName());
            try {
                HamsterComponent comp = (HamsterComponent) c.
                        newInstance();

                onClassLoad(engine);

                Class[] innerC = c.getClasses();
                for (Class innerC1 : innerC) {
                    if (Action.class.isAssignableFrom(innerC1)) {
                        Action action = reconstructAction(innerC1, comp, comp.
                                getClass());
                        if (action != null && action.getStaticName() != null) {
                            LOG.log(Level.INFO, "registering actionClass : {0}  action:{1}  {2}", new Object[]{innerC1.getName(), action.
                                getStaticName(), comp.
                                getClass()});
                            registerStaticAction(action, comp);
                        }
                    }
                }

            } catch (InstantiationException ex) {
                int m = c.getModifiers();
                if (isAbstract(m)) {
                    LOG.log(Level.INFO, "Warning: This HamsterComponent is abstract and will not be cloneable: {0}", c.
                            getName());
                } else {
                    LOG.log(Level.INFO, "HamsterLoader:  This HamsterComponent does not have an default constructor: {0}", c.
                            getName());
                    addError("This HamsterComponent does not have an default constructor: " + c.
                            getName());
                    LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);

                }
            } catch (IllegalAccessException ex) {
                int m = c.getModifiers();
                if (isAbstract(m)) {
                    LOG.log(Level.INFO, "Warning: This HamsterComponent is abstract and will not be cloneable: {0}", c.
                            getName());
                } else {
                    LOG.log(Level.INFO, "HamsterLoader:  This HamsterComponent does not have an default constructor: {0}", c.
                            getName());
                    addError("This HamsterComponent does not have an default constructor: " + c.
                            getName());
                    LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);

                }
            }

        }
       
    }

    public static Action reconstructAction(
            Class ActionClass, HamsterComponent owner, Class ownerClass) {

        try {
            Constructor c = ActionClass.getConstructor(new Class[]{ownerClass});
            Object o = null;
            try {
                o = c.newInstance(new Object[]{owner});
            } catch (IllegalArgumentException ex) {
                LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
            } catch (InvocationTargetException ex) {
                LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
            } catch (IllegalAccessException ex) {
                addError("Action " + ActionClass.getName() + " must be public!");
                LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
            } catch (InstantiationException ex) {
                LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
            }

            return (Action) o;
        } catch (SecurityException ex) {
            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
        } catch (NoSuchMethodException ex) {
            LOG.log(Level.SEVERE, "HamsterLoader:  Action {0} needs a default constructor!", ActionClass.getName());
//            UIEngine.
//                    addError("Action " + ActionClass.getName() + " needs a default constructor!");
//           LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
            // System.exit(2);
        }

        return null;
    }

    private static final Logger LOG = getLogger(HamsterLoader.class.getName());
}
