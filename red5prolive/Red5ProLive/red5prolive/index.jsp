<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ page import="org.springframework.context.ApplicationContext,
				com.red5pro.server.secondscreen.net.NetworkUtil,
				org.springframework.web.context.WebApplicationContext,
				com.red5pro.live.Red5ProLive,
				java.util.List,
				java.net.Inet4Address"%>

<%  
 
	String ip =  NetworkUtil.getLocalIpAddress();
	ApplicationContext appCtx = (ApplicationContext) application.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
	Red5ProLive service = (Red5ProLive) appCtx.getBean("web.handler");
 
	List<String> streamNames = service.getLiveStreams();
	StringBuffer buffer = new StringBuffer();

	if(streamNames.size() == 0) {
		buffer.append("No streams found. Refresh if needed.");
	}
	else {
		buffer.append("<ul>\n");
		for (String streamName:streamNames) {
			buffer.append("<li><a>" + streamName + " on " + ip + "</a></li>\n");
		}
		buffer.append("</ul>\n");
	}

 %>
 
<!doctype html>
<html>
<body>
	<div>
		<h1>Streams on Red5ProLive</h1>
		<%=buffer.toString()%>
	</div>
	<hr>
	<div>
		<h2>Start a broadcast session on your device to see it listed!</h2>
		<p>In the Settings dialog of the <a href="https://github.com/red5pro">Red5 Pro Application</a> enter these values:</p>
		<table>
			<tr>
				<td>Server</td>
				<td><b><%=ip%></b></td>
			</tr>
			<tr>
				<td>App Name</td>
				<td><b>red5prolive</b></td>
			</tr>
			<tr>
				<td>Stream Name</td>
				<td><b>helloWorld</b></td>
			</tr>
		</table>
	</div>
</body>
</html>