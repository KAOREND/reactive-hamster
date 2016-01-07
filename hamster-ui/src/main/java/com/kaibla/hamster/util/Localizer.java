package com.kaibla.hamster.util;

import static com.kaibla.hamster.base.UIContext.getLocale;
import com.kaibla.hamster.persistence.attribute.Attribute;
import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import static java.util.ResourceBundle.getBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author kai
 */
public class Localizer {

    static HashSet<String> bundleNames = new HashSet();
    private static final boolean development=true;
    private static final HashSet<String> collectedKeys=new HashSet();
    private static final ConcurrentHashMap<String,String> cachedTranslations=new ConcurrentHashMap();

    public static void addBundles(String... bundlenames) {
        bundleNames.addAll(asList(bundlenames));
    }

    public static String getLocalizedString(String key, Locale locale) {        
        String cacheKey=key+"_"+locale.hashCode();
        String cached=cachedTranslations.get(cacheKey);
        if(cached != null) {
            return cached;
        }
        for (String s : bundleNames) {   
            try {
                String translation = getBundle(s, locale).getString(key);
                if (translation != null) {
                    cachedTranslations.put(cacheKey, translation);
                    return translation;
                }
            } catch(java.util.MissingResourceException ex) {
               
            }
        }
        if(development) {
            collectedKeys.add(key);
            System.out.println("missing translation keys:");
            for(String missedKey : collectedKeys) {
                System.out.println(missedKey+"=");
            }
        }
        LOG.log(Level.INFO, "Localization Resource was not found for key: {0}", key);
        return key;
    }

    public static String getLocalizedAttrString(Attribute attr, Locale locale) {
        String key = attr.getTableClass().getName() + "." + attr.getName();
        return getLocalizedString(key, locale);
    }

    public static String getLocalizedAttrString(Attribute attr) {
        return getLocalizedAttrString(attr, getLocale());
    }

    public static String getLocalizedString(String key) {
        return getLocalizedString(key, getLocale());
    }
    private static final Logger LOG = getLogger(Localizer.class.getName());
}
