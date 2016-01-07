package com.kaibla.hamster.base;

import com.kaibla.hamster.base.Context;
import com.kaibla.hamster.persistence.model.Document;
import java.util.HashMap;
import java.util.Locale;
import static java.util.Locale.getDefault;
import java.util.Map;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author kai
 */
public class UIContext {

    private final static ThreadLocal<InternalContext> contextMap = new ThreadLocal<InternalContext>();

    private static InternalContext getContext() {
        InternalContext result = contextMap.get();
        if (result == null) {
            result = new InternalContext();
            contextMap.set(result);
        }
        return result;
    }

    public static void clear() {
        contextMap.remove();
    }

    /**
     * @return the request
     */
    public static HttpServletRequest getRequest() {
        return getContext().request;
    }

    /**
     * @param request the request to set
     */
    public static void setRequest(HttpServletRequest request) {
        getContext().request = request;
    }

    /**
     * @return the response
     */
    public static HttpServletResponse getResponse() {
        return getContext().response;
    }

    /**
     * @param response the response to set
     */
    public static void setResponse(HttpServletResponse response) {
        getContext().response = response;
    }

    /**
     * @return the page
     */
    public static HamsterPage getPage() {
        return getContext().page;
    }

    /**
     * @param page the page to set
     */
    public static void setPage(HamsterPage page) {
        getContext().page = page;
        Context.setListenerContainer(page.getListenerContainer());
    }

    /**
     * @return the user
     */
    public static Document getUser() {
        return getContext().user;
    }

    public static void setEvent(AsyncContext event) {
        getContext().event = event;
    }

    public static AsyncContext getEvent() {
        return getContext().event;
    }

    public static Locale getLocale() {
        if (getContext().locale == null) {
            return getDefault();
        } else {
            return getContext().locale;
        }
    }

    public static void setLocale(Locale locale) {
        getContext().locale = locale;
    }

    /**
     * @param user the user to set
     */
    public static void setUser(Document user) {        
        getContext().user = user;
    }

    public static Map getParameterMap() {
        if (getContext().parameterMap == null) {
            getContext().parameterMap = new HashMap();
        }
        return getContext().parameterMap;
    }

    public static void setParameterMap(Map parameterMap) {
        getContext().parameterMap = parameterMap;
    }

    public static void setSession(HamsterSession session) {
        getContext().session = session;
    }

    public static HamsterSession getSession() {
        return getContext().session;
    }
    
    

    private static class InternalContext {
        HttpServletRequest request;
        HttpServletResponse response;
        HamsterPage page;
        Document user;
        AsyncContext event;
        Locale locale;
        Map parameterMap;
        HamsterSession session;
    }
    
    private static final Logger LOG = getLogger(UIContext.class.getName());
}
