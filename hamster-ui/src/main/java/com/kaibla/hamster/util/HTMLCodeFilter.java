/*
 * HTMLCodeFilter.java Created on 26. Februar 2007, 19:16
 */
package com.kaibla.hamster.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;

/**
 * @author Kai Orend
 */
public class HTMLCodeFilter {

    private static AntiSamy rteAntiSamy;
    private static AntiSamy textAntiSamy;
    private static Policy rtePolicy;
    private static Policy textPolicy;
    private static final String urlRegex = "\\(?\\b(https://|http://|www[.])[-A-Za-z0-9+&amp;@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&amp;@#/%=~_()|]";

    static {
        try {

            rtePolicy = Policy.getInstance(HTMLCodeFilter.class.
                    getResourceAsStream("antisamy-hamster.xml"));
            textPolicy = Policy.getInstance(HTMLCodeFilter.class.
                    getResourceAsStream("antisamy-hamster-text.xml"));

            rteAntiSamy = new AntiSamy(rtePolicy);
            textAntiSamy = new AntiSamy(textPolicy);
        } catch (PolicyException ex) {
            Logger.getLogger(HTMLCodeFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String removeBadHTML(String s) {
        String result = s;
        try {
            result = rteAntiSamy.scan(s).getCleanHTML();
            //wrap URLs 
            Pattern patt = Pattern.compile(urlRegex);

            Matcher m = patt.matcher(result);
            StringBuffer sb = new StringBuffer(result.length());
            while (m.find()) {
                String urlString = m.group(0);
                if (!urlString.startsWith("http")) {
                    urlString = "http://" + urlString;
                }
                try {
                    // ... possibly process 'text' ...
                    //verify that the URL is valid:
                    URL url = new URL(urlString);
                    String replacement = "<a href=\"" + urlString + "\" target=\"_blank\">" + urlString + "</a>";
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                } catch (MalformedURLException ex) {
                    //do not wrap as tag if it is not a valid url
                    m.appendReplacement(sb, Matcher.quoteReplacement(urlString));
                }

            }
            m.appendTail(sb);
            return sb.toString();
            //return encode(sb.toString(), "UTF-8");

        } catch (ScanException ex) {
            Logger.getLogger(HTMLCodeFilter.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } catch (PolicyException ex) {
            Logger.getLogger(HTMLCodeFilter.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    public static String removeAllHTML(String s) {
        String result = s;
        try {
            result = textAntiSamy.scan(s).getCleanHTML();
        } catch (ScanException ex) {
            Logger.getLogger(HTMLCodeFilter.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } catch (PolicyException ex) {
            Logger.getLogger(HTMLCodeFilter.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
        return result;
    }

    public static String removeBADHTMLAndAddSmileys(String s) {
        String result = removeBadHTML(s);
        String cssPrefix = "sprite-smileys-";
        result = replaceSmiley(result, cssPrefix, ":-)", "smiley");
        result = replaceSmiley(result, cssPrefix, ":)", "smiley");
        result = replaceSmiley(result, cssPrefix, "(:", "smiley");
        result = replaceSmiley(result, cssPrefix, "(-:", "smiley");
        result = replaceSmiley(result, cssPrefix, ":’-(", "crying");
        result = replaceSmiley(result, cssPrefix, ":'-(", "crying");
        result = replaceSmiley(result, cssPrefix, ":´-(", "crying");
        result = replaceSmiley(result, cssPrefix, ":~-(", "crying");
        result = replaceSmiley(result, cssPrefix, ":'(", "crying");
        result = replaceSmiley(result, cssPrefix, ":-|", "grimm");
        result = replaceSmiley(result, cssPrefix, ":-(", "unhappy");
        result = replaceSmiley(result, cssPrefix, ":(", "unhappy");
        result = replaceSmiley(result, cssPrefix, ";-)", "wink");
        result = replaceSmiley(result, cssPrefix, ";)", "wink");
        result = replaceSmiley(result, cssPrefix, "‘-)", "wink");
        result = replaceSmiley(result, cssPrefix, ":-D", "grinn");
        result = replaceSmiley(result, cssPrefix, ":D", "grinn");
        result = replaceSmiley(result, cssPrefix, ":P", "tounge");
        result = replaceSmiley(result, cssPrefix, ":-P", "tounge");
        return result;
    }

    private static String replaceSmiley(String source, String cssPrefix, String pattern, String name) {
        return source.replaceAll(Pattern.quote(pattern), "<div class=\"" + cssPrefix + name + "\"> </div>");
    }

    /*
     * Replace all < and > chars and parse BBCode (in a future release) .
     */
    private static String getFilterString(String s) {
        if (s == null) {
            return "";
        }
        // s=s.replaceAll("<","&lt;");
        // s=s.replaceAll(">","&gt;");
        try {
	//    System.out.print("vor encode: " + s);

            // LOG.info("nach encode:1
            // "+java.net.URI.create(s).toASCIIString());
            // s = URLEncoder.encode(s, "ISO-8859-1");
            //filter out bad html code..
            s = encode(s, "UTF-8");
            //    LOG.info("nach encode:1 "+s);
            s=StringUtils.replace(s, "+",  "%20");
            //s=s = s.replaceAll("\\+", "%20");
            //s = s.replaceAll("\\+", "%20");
            //s = s.replaceAll("%0A", "<br/>");
            // LOG.info("nach encode:2 "+s);
            // java.net.URI.create(s).toASCIIString()
            //	LOG.info(" --> " + s);
        } catch (UnsupportedEncodingException ex) {
            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
        }
        return s;
    }

    /*
     * Replace all < and > chars and parse BBCode (in a future release) .
     */
    public static String getFilteredStringWithBR(String s) {
        if (s == null) {
            return "";
        }
        // s=s.replaceAll("<","&lt;");
        // s=s.replaceAll(">","&gt;");
        try {
	  //  System.out.print("vor encode: " + s);

            // LOG.info("nach encode:1
            // "+java.net.URI.create(s).toASCIIString());
            // s = URLEncoder.encode(s, "ISO-8859-1");
            s = encode(s, "UTF-8");
            //LOG.info("nach encode:1 "+s);
            s = s.replaceAll("\\+", "%20");
            s = s.replaceAll("%0A", encode("<br/>", "UTF-8"));
            // LOG.info("nach encode:2 "+s);
            // java.net.URI.create(s).toASCIIString()
            //	LOG.info(" --> " + s);
        } catch (UnsupportedEncodingException ex) {
            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
        }
        return s;
    }

    public static String decode(String s) {
        s = s.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
        s = s.replaceAll("\\+", "%2B");
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HTMLCodeFilter.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
    }

    public static String cutString(String s, int maxLength) {
        String e = decode(s);
        if (e.length() > maxLength) {
            return getStrictFilteredString(e.substring(0, maxLength) + "...");
        } else {
            return s;
        }

    }

    public static String getStrictFilteredString(String s) {
        return getFilterString(s);
    }

    public static String getRichFilteredString(String s) {
        return getFilterString(s);
    }
    private static final Logger LOG = getLogger(HTMLCodeFilter.class.getName());

}
