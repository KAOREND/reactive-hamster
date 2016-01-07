/*
 * Template.java Created on 18. Februar 2007, 17:36
 */
package com.kaibla.hamster.util;

import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.base.Resumable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import static java.nio.charset.Charset.forName;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.compile;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import static javax.xml.parsers.SAXParserFactory.newInstance;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Kai Orend
 */
public class Template implements Serializable, Resumable {
    
    private final boolean debug = false;
    /**
     * the first index denotes the language, the second the part of the template
     */
    private String templateParts[] = null;
    private String templatePartsXML[][] = null;
    private URL url = null;
    // will be replaced soon...
    // private String templateParts[] = null;

    // ! TODO: neue zeilen am template-anfang filtern
    public static Pattern htmlComment = compile("( |\\t)*?<!\\-\\-(.|\\n)*?\\-\\->( |\\t)*(\\n)*");
    public static Pattern whitespace = compile(">( |\\t)*?<");
    public static Pattern doubleNewline = compile("((\\n| |\\t)*\\n)");
    public static Pattern emptyLine = compile("^\\n");
    private boolean isXML = false;
    private boolean[] aslots = new boolean[0];
    private boolean convert = true;
    private int slotCount = 0;
    private String source = "";
    private static HashMap<String, Template> templates = new HashMap();
    
    public Template() {
    }
    
    public Template(URL url) {
        this.url = url;
        templates.put(url.toString(), this);
        try {
            load();
        } catch (Exception ex) {
            Logger.getLogger(Template.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }
    
    public Template(URL url, boolean convert) {
        this.url = url;
        this.convert = convert;
        templates.put(url.toString(), this);
         try {
            load();
        } catch (Exception ex) {
            Logger.getLogger(Template.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }
    
    public String getSource() {
        return source;
    }
    
    public int countSlots() {
        return slotCount;
    }
    
    private boolean checkSilentXML(String s) {
        if (true) {
            return false;
        }
        try {
            SAXParser saxParser = newInstance().newSAXParser();
            saxParser.getXMLReader().parse(new InputSource((new StringReader(s))));
            return true;
        } catch (ParserConfigurationException ex) {
            //ex.printStackTrace();
            //  LOG.info("Template Fehler in XML aus Template " + url);
            //LOG.info(s);
        } catch (SAXException ex) {
            //ex.printStackTrace();
            //  LOG.info("Template Fehler in XML aus Template " + url);
            //LOG.info(s);
        } catch (IOException ex) {
            //ex.printStackTrace();
            //  LOG.info("Template Fehler in XML aus Template " + url);
            //LOG.info(s);
        }
        return false;
    }
    
    private boolean checkXML(String s) {
        s = s.replaceAll("<br>", "<br/>");
        try {
            SAXParser saxParser = newInstance().newSAXParser();
            saxParser.getXMLReader().parse(new InputSource((new StringReader(s))));
            return true;
        } catch (ParserConfigurationException ex) {
            //ex.printStackTrace();
            LOG.log(Level.INFO, "Template Fehler in XML aus Template {0}", url);
            //LOG.info(s);

        } catch (SAXException ex) {
            //ex.printStackTrace();
            LOG.log(Level.INFO, "Template Fehler in XML aus Template {0}", url);
            //LOG.info(s);
        } catch (IOException ex) {
            //ex.printStackTrace();
            LOG.log(Level.INFO, "Template Fehler in XML aus Template {0}", url);
            //LOG.info(s);
        }
        return false;
    }
    
    public void load() throws Exception {
        // LOG.info("Template.load() called :-) url is " + url);

        InputStream in = url.openStream();
        
        StringBuilder buf = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(in, forName("UTF8")));
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.isEmpty()) {
                buf.append(line).append("\n");
            }
            // buf.append(line);
        }
        in.close();
        
        load(buf.toString());
    }
    
    public void load(String source) {
        this.source = source;
        //    LOG.info("stripping: "+this.url);
        String buffer = "";
        if (convert) {
            buffer = stripHTMLComments(source.toString());
            
        } else {
            buffer = source.toString();
        }
        boolean aslot = false;
        if (buffer.indexOf("ASLOT") > 0) {
            aslot = true;
//            LOG.info("uses aslot");
        }
        
        LinkedList slots = new LinkedList();
        int slot = buffer.indexOf("SLOT");
        int asloti = buffer.indexOf("ASLOT");
        while (slot != -1 || asloti != -1) {
            int index = -1;
            if ((slot < asloti || asloti == -1) && slot != -1) {
                index = slot;
                //LOG.info("SLOT");
                slots.add(false);
            } else if (asloti != -1) {
                index = asloti;
                //LOG.info("ASLOT");
                slots.add(true);
            }
            if (index != -1) {
                slot = buffer.indexOf("SLOT", index + 2);
                asloti = buffer.indexOf("ASLOT", index + 2);
            } else {
                slot = -1;
                asloti = -1;
            }
        }
        aslots = new boolean[slots.size()];
        Iterator iter = slots.iterator();
        int i = 0;
        while (iter.hasNext()) {
            aslots[i] = ((Boolean) iter.next()).booleanValue();
            i++;
        }
        
        String xmlBuffer = buffer.replace("CLOSEATAG", "</as>");
        xmlBuffer = xmlBuffer.replace("ASLOT", "<as>");
        // xmlBuffer = "<div>" + xmlBuffer + "</div>";
        isXML = checkSilentXML(xmlBuffer);
        if (isXML) {
            //  LOG.info("Template : " + url + " is cachable");
        } else {
            // LOG.info("Template : " + url + " is not cachable CLOSATAG: " + xmlBuffer.contains("</as>"));
        }
        if (convert) {
            
            buffer = buffer.replace("%", "%25");
            buffer = buffer.replace("ö", "%C3%B6");
            buffer = buffer.replace("ü", "%C3%BC");
            buffer = buffer.replace("ß", "%C3%9F");
            buffer = buffer.replace("ä", "%C3%A4");
            
            buffer = buffer.replace("Ö", "%C3%96");
            buffer = buffer.replace("Ü", "%C3%9C");
            buffer = buffer.replace("Ä", "%C3%84");
            
            xmlBuffer = buffer.replace("%", "%25");
            xmlBuffer = xmlBuffer.replace("CLOSEATAG", "</as>");
            xmlBuffer = xmlBuffer.replace("ASLOT", "<as>");
            xmlBuffer = xmlBuffer.replace("ö", "%C3%B6");
            xmlBuffer = xmlBuffer.replace("ü", "%C3%BC");
            xmlBuffer = xmlBuffer.replace("ß", "%C3%9F");
            xmlBuffer = xmlBuffer.replace("ä", "%C3%A4");
            
            xmlBuffer = xmlBuffer.replace("Ö", "%C3%96");
            xmlBuffer = xmlBuffer.replace("Ü", "%C3%9C");
            xmlBuffer = xmlBuffer.replace("Ä", "%C3%84");
            
        }
//        if (aslot) {
//            LOG.info("xmlBuffer:" + url + " \n " + xmlBuffer);
//        }

        String[] parts = xmlBuffer.split("SLOT");
        slotCount = parts.length - 1;
        // old line, kept for the time being to keep it working
        // templateParts = buffer.split("SLOT");
        int langs = 1;
        
        templatePartsXML = new String[langs][parts.length];
        for (int language = 0; language < langs; language++) {
            for (int part = 0; part < parts.length; part++) {
                templatePartsXML[language][part] = parts[part];
                
            }
        }
        
        buffer = buffer.replace("CLOSEATAG", "</a>");
        buffer = buffer.replace("ASLOT", "SLOT");
        parts = buffer.split("SLOT");
        templateParts = new String[parts.length];
        for (int part = 0; part < parts.length; part++) {
            templateParts[part] = parts[part];
        }
    }
    
    public String mergeStrings(HamsterPage page, Object... obj) {
        return mergeStrings(obj, page);
    }
    
    public String mergeStrings(Object[] obj, HamsterPage page) {
        if (page == null) {
            return "Template: page was null Do not use the default Constructor to create a new Instance of an HamsterComponent!";
        }
        if (page.isTemplateCache() && page.makingXMLRequest() && isXML) {
            StringBuilder buf = new StringBuilder();
            if (!page.isTemplateInCache(this)) {
                page.getModificationManager().addTemplate(this);
                page.markTemplateAsCached(this);
            }
            buf.append("<t id=\"").append(this.hashCode()).append("\">");
            for (int i = 0; i < obj.length; i++) {
                buf.append("<s>").append(obj[i]);
                if (aslots[i]) {
                    buf.append("</a>");
                }
                buf.append("</s>");
            }
            buf.append("</t>");
            if (debug) {
                checkXML(buf.toString());
            }
            return buf.toString();
        } else {
            
            int i2 = 0;
            StringBuilder buf = new StringBuilder();
            for (String item : templateParts) {
                buf.append(item);
                if (i2 < obj.length) {
                    buf.append(obj[i2]);
                    i2++;
                }
            }
            // LOG.info("mergeStings: "+buf.toString());
            if (debug) {
                checkXML(buf.toString());
            }
            return buf.toString();
        }
    }
    
    public String getTemplate(HamsterPage page) {
        StringBuilder buf = new StringBuilder();
        buf.append("<template id=\"").append(this.hashCode()).append("\">");
        for (int i = 0; i < templatePartsXML[0].length; i++) {
            buf.append(templatePartsXML[0][i]);
            if (i < templatePartsXML[0].length - 1) {
                buf.append("SLOT");
            }
        }
        buf.append("</template>");
        if (debug) {
            checkXML(buf.toString());
        }
        return buf.toString();
    }
    
    private void checkTemplateCache(HamsterPage page) {
        if (!page.isTemplateInCache(this)) {
            page.getModificationManager().addTemplate(this);
            page.markTemplateAsCached(this);
        }
    }
    
    public String mergeStrings(String s, HamsterPage page) {
        if (page == null) {
            return "Template: page was null Do not use the default Constructor to create a new Instance of an HamsterComponent!";
        }
        
        if (page.isTemplateCache() && page.makingXMLRequest() && isXML) {
            checkTemplateCache(page);
            StringBuilder buf = new StringBuilder();
            
            buf.append("<t id=\"").append(this.hashCode()).append("\">");
            buf.append("<s>").append(s);
            if (aslots[0]) {
                buf.append("</a>");
            }
            buf.append("</s>");
            buf.append("</t>");
            if (debug) {
                checkXML(buf.toString());
            }
            return buf.toString();
        } else {
            StringBuilder buf = new StringBuilder();
            if (templateParts.length > 0) {
             
                buf.append(StringUtils.replace(templateParts[0], "RELATIVE_PATH", page.getRelativePath()));
            }
            buf.append(s);
            for (int i = 1; i < templateParts.length; i++) {
                buf.append(StringUtils.replace(templateParts[i],"RELATIVE_PATH", page.getRelativePath()));
            }
            if (debug) {
                checkXML(buf.toString());
            }
            return buf.toString();
        }
    }
    
    public String mergeStrings(Collection c, HamsterPage page) {
//        if (page == null) {
//            return "Template: page was null Do not use the default Constructor to create a new Instance of an HamsterComponent!";
//        }
        if (page != null && page.isTemplateCache() && page.makingXMLRequest() && isXML) {
            StringBuilder buf = new StringBuilder();
            checkTemplateCache(page);
            buf.append("<t id=\"").append(this.hashCode()).append("\">");
            int i = 0;
            Iterator iter = c.iterator();
            while (iter.hasNext()) {
                buf.append("<s>").append(iter.next());
                if (aslots[i]) {
                    buf.append("</a>");
                }
                buf.append("</s>");
                i++;
            }
            
            buf.append("</t>");
            return buf.toString();
        } else {
            StringBuilder buf = new StringBuilder();
            Iterator iter = c.iterator();
            
            for (String item : templateParts) {
                if (page != null) {
                    buf.append(StringUtils.replace(item,"RELATIVE_PATH", page.getRelativePath()));
                } else {
                    buf.append(item);
                }
                if (iter.hasNext()) {
                    buf.append(iter.next());
                }
            }
            if (debug) {
                checkXML(buf.toString());
            }
            return buf.toString();
        }
    }
    
    public String mergeStrings(Collection c, HamsterPage page, String replace, String replacement) {
//        if (page == null) {
//            return "Template: page was null Do not use the default Constructor to create a new Instance of an HamsterComponent!";
//        }
        if (page != null && page.isTemplateCache() && page.makingXMLRequest() && isXML) {
            StringBuilder buf = new StringBuilder();
            checkTemplateCache(page);
            buf.append("<t id=\"").append(this.hashCode()).append("\">");
            int i = 0;
            Iterator iter = c.iterator();
            while (iter.hasNext()) {
                buf.append("<s>").append(iter.next());
                if (aslots[i]) {
                    buf.append("</a>");
                }
                buf.append("</s>");
                i++;
            }
            
            buf.append("</t>");
            return buf.toString();
        } else {
            StringBuilder buf = new StringBuilder();
            Iterator iter = c.iterator();
            
            for (String item : templateParts) {
                buf.append(StringUtils.replace(item,replace, replacement));
                if (iter.hasNext()) {
                    buf.append(iter.next());
                }
            }
            if (debug) {
                checkXML(buf.toString());
            }
            return buf.toString();
        }
    }
    
    public String getString(HamsterPage page) {
        if (page == null) {
            return "Template: page was null Do not use the default Constructor to create a new Instance of an HamsterComponent!";
        }
        
        if (page.isTemplateCache() && page.makingXMLRequest() && isXML) {
            StringBuilder buf = new StringBuilder();
            checkTemplateCache(page);
            buf.append("<t id=\"").append(this.hashCode()).append("\">");
            buf.append("</t>");
            if (debug) {
                checkXML(buf.toString());
            }
            return buf.toString();
        } else {
            StringBuilder buf = new StringBuilder();
            for (String item : templateParts) {
                buf.append(item.replaceAll("RELATIVE_PATH", page.getRelativePath()));
            }
            if (debug) {
                checkXML(buf.toString());
            }
            return buf.toString();
        }
    }
    
    public static String stripHTMLComments(String input) {
        // LOG.info("stripping: " + input);

        try {
            
            input = htmlComment.matcher(input).replaceAll("");
            // input = whitespace.matcher(input).replaceAll("><");
            // input = doubleNewline.matcher(input).replaceAll("\n");
            // input = emptyLine.matcher(input).replaceAll("");
        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            //LOG.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            LOG.log(Level.INFO, "Beim Auskommentieren von: {0} ist was schiefgelaufen!", input);
        }
        return input;
    }
    
    public static void main(String[] args) {
        String input = "";
        LOG.log(Level.INFO, "''{0}''", stripHTMLComments(input));
    }
    
    protected Object writeReplace() {
        return new PlaceHolder(this.url.toString());
    }
    
    @Override
    public void resume() {
        try {
            load();
        } catch (Exception ex) {
            Logger.getLogger(Template.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static class PlaceHolder implements Serializable {
        
        String url;
        
        public PlaceHolder(String url) {
            this.url = url;
        }
        
        protected Object readResolve() throws ObjectStreamException {
            Template template = templates.get(url);
            if (template == null) {
                try {
                    template = new Template(new URL(url));
                    Logger.getLogger(Template.class.getName()).info("reloading template after resume " + url);
                    template.load();
                } catch (MalformedURLException ex) {
                    Logger.getLogger(Template.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    Logger.getLogger(Template.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
            return templates.get(url);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (String item : templateParts) {
            buf.append(item);
        }
        if (debug) {
            checkXML(buf.toString());
        }
        return buf.toString();
    }
    
    private static final Logger LOG = getLogger(Template.class.getName());
}
