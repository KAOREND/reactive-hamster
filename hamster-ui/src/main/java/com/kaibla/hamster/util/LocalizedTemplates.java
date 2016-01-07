package com.kaibla.hamster.util;

import com.kaibla.hamster.base.UIContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kai
 */
public class LocalizedTemplates {

    String name;
    Class sourceClass;
    HashMap<String,Template> localTemplates=new HashMap<String,Template>();
    HashSet<String> missing=new HashSet();
    Template defaultTemplate=null;
    String suffix;
    public LocalizedTemplates(Class sourceClass,String name,String suffix) {
        this.name = name;
        this.sourceClass = sourceClass;
        this.suffix=suffix;
        defaultTemplate= new Template(sourceClass.getResource(name+suffix),false);
        try {
            defaultTemplate.load();
        } catch (Exception ex) {
            Logger.getLogger(LocalizedTemplates.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public synchronized Template  getLocalizedTemplate() {
          return getLocalizedTemplate(UIContext.getLocale());
     }
    
    public synchronized Template  getLocalizedTemplate(Locale locale) {        
        String localeKey = locale.getLanguage();
        Template result=localTemplates.get(localeKey);
        if(result != null) {
            return result;
        } else if(!missing.contains(localeKey)) {
            try {
                result = new Template(sourceClass.getResource(name+"_"+localeKey+""+suffix),false);
                result.load();
                localTemplates.put(localeKey, result);
                return result;
            } catch(Exception ex) {
               missing.add(localeKey);
            }
        }
        
        return defaultTemplate;
    }
    
}
