<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
import="java.util.*,java.util.concurrent.*,
org.springframework.context.ApplicationContext,
org.springframework.web.context.WebApplicationContext,
com.red5pro.mpegts.plugin.TSIngestPlugin,
com.red5pro.mpegts.plugin.TSIngestEndpoint,
com.infrared5.red5pro.tsingest.Red5ProTSIngest"
%>
<%
    ApplicationContext appCtx = (ApplicationContext) getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    Red5ProTSIngest tsApp = (Red5ProTSIngest) appCtx.getBean("web.handler");    
    // handle any submissions
    String message = "";
    // form action (create or dispose)
    String action = request.getParameter("action");
    String ipAddress = request.getParameter("ip");
    int port = (request.getParameter("port") != null) ? Integer.valueOf(request.getParameter("port")) : 1024;
    // check action for how to proceed
    if ("create".equals(action) && ipAddress != null) {
	    boolean unicast = (request.getParameter("cast") != null) ? "unicast".equals(request.getParameter("cast")) : false;
    	String streamName = request.getParameter("streamName");
	    // if we have parameters, submit to the app
	    String streamPath = null;
	    if (streamName != null && !"".equals(streamName)) {
		    streamPath = tsApp.createListenerEndpoint(unicast, ipAddress, port, streamName);
		    if (streamPath != null) {
		    	message = "Stream created at " + streamPath;
		    }
	    }
    } else if ("kill".equals(action) && ipAddress != null) {
	    if (tsApp.disposeEndpoint(ipAddress, port)) {
	    	message = String.format("Stream disposed at %s:%d", ipAddress, port);
	    } else {
	    	message = String.format("Stream dispose failed for %s:%d", ipAddress, port);
	    }
    }
    // get the established transport end-points
    ConcurrentMap<String, TSIngestEndpoint> endPoints = tsApp.list();
%>
<!doctype html>
<html lang="eng">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Project Index">
    <link rel="stylesheet" href="css/main.css">
    <link href="http://fonts.googleapis.com/css?family=Lato:400,700" rel="stylesheet" type="text/css">
    <title>Red5 Pro MPEG-TS Demo</title>
    <style>
    </style>
  </head>
  <body>
    <p>
        <h1>MPEG-TS Demo</h1>
        <div><h3><%= message %></h3></div>
        <h2>Streams</h2>
        <ul>
        <%
        if (endPoints.isEmpty()) {
	    %>
	        	<li>None active</li>
	    <%
        } else {
	        for (Map.Entry<String, TSIngestEndpoint> entry : endPoints.entrySet()) {
	            String id = entry.getKey();
	            TSIngestEndpoint val = entry.getValue();
	    %>
	        	<li><a href="/live/viewer.jsp?stream=<%= val.getStreamName() %>" target="_blank"><%= val.getStreamName() %></a> <b>Type:</b> <%= val.isUnicast() ? "Unicast" : "Multicast" %> <b>Connected:</b> <%= val.isConnected() %></li>
	    <%
	        }
	    }
        %>
        </ul>
        <h2>New Stream</h2>
        <form method="POST">
            Group / IP: <input type="text" name="ip" value="239.0.0.1" size="16" maxlength="15"/><br />
            Port: <input type="number" name="port" value="1024" min="1024" max="65535"/><br />
            Type: <input type="radio" name="cast" value="multicast" checked> Multicast <input type="radio" name="cast" value="unicast"> Unicast<br />
            Stream name: <input type="text" name="streamName" value="test" size="16" maxlength="16"/><br />
            <input type="hidden" name="action" value="create"/>
            <input type="submit" value="Submit"/>
        </form>
        <h2>Remove Stream</h2>
        <form method="POST">
            Group / IP: <input type="text" name="ip" value="239.0.0.1" size="16" maxlength="15"/><br />
            Port: <input type="number" name="port" value="1024" min="1024" max="65535"/><br />
            <input type="hidden" name="action" value="kill"/>
            <input type="submit" value="Submit"/>
        </form>
    </p>
    <script>
        // prevent resubmission via refresh
		if (window.history.replaceState) {
            window.history.replaceState(null, null, window.location.href);
        }
    </script>
  </body>
</html>