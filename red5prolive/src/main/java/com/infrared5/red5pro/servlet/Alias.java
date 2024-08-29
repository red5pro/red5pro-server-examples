package com.infrared5.red5pro.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.red5.server.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.red5pro.server.IProScopeHandler;
import com.red5pro.server.stream.IAliasProvider;

/**
 * Provide http/s access to aliasing CRUD.
 *
 * @author Paul Gregoire
 */
public class Alias extends HttpServlet {

    private static final long serialVersionUID = 33212246211666L;

    private static Logger log = LoggerFactory.getLogger(Alias.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // get the application context for the app in which this servlet is running
        ApplicationContext appCtx = (ApplicationContext) getServletContext()
                .getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        // if theres no context then this is not running in a red5 app
        if (appCtx == null) {
            // return an error
            resp.sendError(500, "No application context found");
        } else {
            // get the pro handler, if it fails the interface wasn't implemented
            IProScopeHandler app = (IProScopeHandler) appCtx.getBean("web.handler");
            // get the provider
            IAliasProvider provider = app.getStreamNameAliasProvider();
            if (provider != null) {
                log.info("Found alias provider: {}", provider);
                // example:
                // http://localhost:5080/live/alias/addStreamAlias?streamName=paul1&alias=paul2
                // http://localhost:5080/live/alias/resolvePlayAlias?alias=paul2
                // get the action add, remove, resolve with type
                String requestUri = req.getRequestURI();
                log.info("Request: {}", requestUri);
                int lastSlashIdx = Math.max(0, requestUri.lastIndexOf("/"));
                int endIdx = requestUri.indexOf('?');
                requestUri = endIdx > 0 ? requestUri.substring(lastSlashIdx, endIdx) : requestUri.substring(lastSlashIdx);
                log.info("Request: {}", requestUri);
                String streamName = req.getParameter("streamName");
                // whether its a stream or play alias depends on the method call
                String alias = req.getParameter("alias");
                log.info("Stream name: {} alias: {}", streamName, alias);
                switch (requestUri) {
                    case "/addStreamAlias":
                        if (provider.addStreamAlias(alias, streamName)) {
                            log.info("Stream alias added", alias);
                        } else {
                            log.warn("Stream alias add failed");
                            resp.sendError(500, "Stream alias add failed");
                        }
                        break;
                    case "/addPlayAlias":
                        if (provider.addPlayAlias(alias, streamName)) {
                            log.info("Play alias added", alias);
                        } else {
                            log.warn("Play alias add failed");
                            resp.sendError(500, "Play alias add failed");
                        }
                        break;
                    case "/removeStreamAlias":
                        if (provider.removeStreamAlias(alias)) {
                            log.info("Stream alias removed", alias);
                        } else {
                            log.warn("Stream alias remove failed");
                            resp.sendError(500, "Stream alias remove failed");
                        }
                        break;
                    case "/removePlayAlias":
                        if (provider.removePlayAlias(alias)) {
                            log.info("Play alias removed", alias);
                        } else {
                            log.warn("Play alias remove failed");
                            resp.sendError(500, "Play alias remove failed");
                        }
                        break;
                    case "/resolveStreamAlias":
                        streamName = provider.resolveStreamAlias(alias, new TempConnection(requestUri));
                        if (streamName != null) {
                            log.info("Stream alias: {} resolves to: {}", alias, streamName);
                            resp.getOutputStream().print(streamName);
                        } else {
                            log.warn("Stream alias resolve failed");
                            resp.sendError(500, "Stream alias resolve failed");
                        }
                        break;
                    case "/resolvePlayAlias":
                        streamName = provider.resolvePlayAlias(alias, new TempConnection(requestUri));
                        if (streamName != null) {
                            log.info("Play alias: {} resolves to: {}", alias, streamName);
                            resp.getOutputStream().print(streamName);
                        } else {
                            log.warn("Play alias resolve failed");
                            resp.sendError(500, "Play alias resolve failed");
                        }
                        break;

                }
            } else {
                log.warn("No StreamNameAliasProvider found");
            }
        }
    }

    private class TempConnection extends BaseConnection {

        public TempConnection(String path) {
            this.path = path;
        }

        @Override
        public Encoding getEncoding() {
            throw new UnsupportedOperationException("Unimplemented method 'getEncoding'");
        }

        @Override
        public void ping() {
            throw new UnsupportedOperationException("Unimplemented method 'ping'");
        }

        @Override
        public int getLastPingTime() {
            throw new UnsupportedOperationException("Unimplemented method 'getLastPingTime'");
        }

        @Override
        public void setBandwidth(int mbits) {
            throw new UnsupportedOperationException("Unimplemented method 'setBandwidth'");
        }

        @Override
        public String getProtocol() {
            throw new UnsupportedOperationException("Unimplemented method 'getProtocol'");
        }

        @Override
        public long getReadBytes() {
            throw new UnsupportedOperationException("Unimplemented method 'getReadBytes'");
        }

        @Override
        public long getWrittenBytes() {
            throw new UnsupportedOperationException("Unimplemented method 'getWrittenBytes'");
        }

    }

}
