package com.kaibla.hamster.servlet;

import com.kaibla.hamster.base.UIEngine;
import java.io.IOException;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Kai Orend
 */
public class CometProcessor extends HttpServlet {

    private static final long serialVersionUID = 365737675389366477L;
    private static UIEngine engine;
    private static CometProcessor self = null;
    private final long maxTime = 0;
    private static final String ENGINE_CLASS="ENGINE_CLASS";
    public CometProcessor() {
      
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if(UIEngine.getEngine() == null) {
        String className=config.getInitParameter(ENGINE_CLASS);
        engine=UIEngine.loadEngine(className);
        } else {
           engine=UIEngine.getEngine();
        }
        self=this;
        
    }
    
    public static UIEngine getEngine() {
        return engine;
    }

    public static void setEngine(UIEngine engine) {
        CometProcessor.engine = engine;
    }
    
    public static CometProcessor getServlet() {
        return self;
    }
    

    @Override
    public void destroy() {
        engine.destroy();
        super.destroy();
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        doService(req, resp);
    }

    public void doService(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        // create the async context, otherwise getAsyncContext() will be null
        final AsyncContext ctx = req.startAsync();

        // set the timeout
        ctx.setTimeout(30000);

        // attach listener to respond to lifecycle events of this AsyncContext
        ctx.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                engine.removeConnection(ctx);
//                log("onComplete called");
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
//                log("onTimeout called");               
                engine.removeConnection(ctx);
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                log("onError called");
                log("" + event.getThrowable());
                engine.removeConnection(ctx);
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
//                log("onStartAsync called");
            }
        });

        ctx.start(new Runnable() {

            @Override
            public void run() {
                resp.setCharacterEncoding("UTF-8");
                engine.doRequest(req, resp, ctx);
                resp.setCharacterEncoding("UTF-8");
            }
        });
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doService(req, resp);
    }
    private static final Logger LOG = getLogger(CometProcessor.class.getName());
}
