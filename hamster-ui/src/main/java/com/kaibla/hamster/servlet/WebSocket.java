/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * .
 */
package com.kaibla.hamster.servlet;

import com.kaibla.hamster.base.UIContext;
import static com.kaibla.hamster.base.UIContext.setPage;
import static com.kaibla.hamster.base.UIContext.setUser;
import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.UIEngine;
import com.kaibla.hamster.base.HamsterPage;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newFixedThreadPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.websocket.EndpointConfig;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;


/**
 *
 * @author kai
 */
@javax.websocket.server.ServerEndpoint("/websocket")
public class WebSocket {

    
    @javax.websocket.OnMessage
    public void onMessage(final Session session, String message) {
        LOG.info(message);
        long startTime = System.currentTimeMillis();
        final UIEngine engine = CometProcessor.getEngine();
        engine.doLazyInit();
        try {
            String query = message.substring(0, message.indexOf("&"));
            String parameters = message.substring(message.indexOf("&"), message.length() - 1);
            final HashMap<String, String[]> parameterMap = new HashMap();
            for (String parameter : parameters.split("&")) {
                int equalsIndex = parameter.indexOf("=");
                if (equalsIndex != -1) {
                    String name = parameter.substring(0, equalsIndex);
                    String value = parameter.substring(parameter.indexOf("=") + 1);
                    parameterMap.put(name, new String[]{HamsterComponent.decode(value)});
                }
            }

            StringTokenizer t = new StringTokenizer(query, "?");
            String commandTemp = t.nextToken();
            if (commandTemp.length() != 1) {
                commandTemp = t.nextToken();
            }
            final String command = commandTemp;
            String pageKey = t.nextToken();
            final String componentKey = t.nextToken();
            final LinkedList<String> params = new LinkedList();
            while (t.hasMoreTokens()) {
                params.add(t.nextToken());
            }
            final HamsterPage page = engine.getPage(pageKey);

            if (page == null) {
                sendMessage(session, "failed to lookup persisted session. Reload the page", new SendHandler() {
                    @Override
                    public void onResult(SendResult sr) {

                    }
                });
                return;
            }
            page.setWebSocketSession(session);
            engine.executeSynchronously(new Runnable() {

                @Override
                public void run() {
                    UIContext.setParameterMap(parameterMap);

                    HamsterComponent comp = page.getComponentMap().get(componentKey);
                    if (comp == null) {
                        LOG.warning("component could not be found: " + componentKey + "  in page " + page.getId());
                    }
                    setPage(page);
                    UIContext.setSession(page.getSession());
                    UIContext.setLocale(page.getSession().getLocale());
                    setUser(page.getUser());
                    //           synchronized (page) {          
                    page.markAsAlive();
                    String response = "";
                    if (comp != null) {
                        response = engine.handleAjaxRequest(page, comp, command, params);
                    } else {
                        page.updateAll();
                    }
                    UIContext.clear();

                    final int confirmCount = page.getModificationManager().getConfirmationCounter();
                    final HamsterPage myPage = page;
                    //session.getBasicRemote().sendText(response);
                    if (response != null) {
                        sendMessage(session, response, new SendHandler() {
                            @Override
                            public void onResult(SendResult sr) {
                                if (sr.isOK()) {
                                    try {
                                        if (!myPage.getLock().tryLock(10, TimeUnit.SECONDS)) {
                                            Logger.getLogger(WebSocket.class.getName()).log(Level.SEVERE, "Could not aquire page lock after 10 seconds");
                                            return;
                                        }

                                    } catch (InterruptedException ex) {
                                        Logger.getLogger(WebSocket.class.getName()).log(Level.SEVERE, null, ex);
                                        return;
                                    }
                                    try {
                                        myPage.getModificationManager().confirmLastModificationXML(confirmCount);
                                    } finally {
                                        myPage.getLock().unlock();
                                    }
                                } else {
                                    getLogger(WebSocket.class.
                                            getName()).
                                            log(Level.SEVERE, null, sr.getException());
                                }
                            }
                        });
                    }
                }
            }, page.getListenerContainer(),true);

        } catch (Throwable th) {
            Logger.getLogger(WebSocket.class.getName()).log(Level.SEVERE, null, th);
        } finally {
            UIContext.clear();

        }
        long endTime = System.currentTimeMillis();
        engine.reportRequestProcssing(startTime, endTime);
    }

//     @javax.websocket.OnMessage
//    public void onClose() {
//        LOG.warning("websocket closed"); 
//    }
    /** 
     * sendMessage is executed snychronously to avoid tomcat nativelib crashes.  
     * @param session
     * @param message
     * @param handler 
     */
    public synchronized static void sendMessage(final Session session, final String message, final SendHandler handler) {
//        synchronized (session) {
            try {
                session.getBasicRemote().sendText(message);
                handler.onResult(new SendResult());
            } catch (IOException ex) {
                Logger.getLogger(WebSocket.class.getName()).log(Level.SEVERE, null, ex);
                handler.onResult(new SendResult(ex));
                try {
                    //close broken session
                    session.close();
                } catch (IOException ex1) {
                    Logger.getLogger(WebSocket.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
//        }
//        }
    }

//    public static void sendMessage(final Session session, final String message, final SendHandler handler, boolean async) {
//        if(async) {
//            sendMessageAsync(session, message, handler);
//        } else {
//            sendMessage(session, message, handler);
//        }
//    }
//    public static void sendMessageAsync(final Session session, final String message, final SendHandler handler) {
//        executer.execute(new Runnable() {
//            @Override
//            public void run() {
//                synchronized (session) {
//                    try {
//                        session.getBasicRemote().sendText(message);
//                        handler.onResult(new SendResult());
//                    } catch (IOException ex) {
//                        Logger.getLogger(WebSocket.class.getName()).log(Level.SEVERE, null, ex);
//                        handler.onResult(new SendResult(ex));
//                    }
//                }
//            }
//        });
//    }
    private static final Logger LOG = getLogger(WebSocket.class.getName());

}
