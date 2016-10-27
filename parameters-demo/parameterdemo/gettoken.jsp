<%@ page import="org.springframework.context.ApplicationContext,
			demo.red5pro.parameters.security.Red5ProLive,
			org.springframework.web.context.WebApplicationContext"%>
<%

	String token = "nope";
	String userId=null;
	String type=null;
	String broadcastId=null;
	
	if(request.getParameter("userId")!=null) {
		userId=request.getParameter("userId");
	}
	
	if(request.getParameter("type")!=null) {
		type=request.getParameter("type");
	}
	if(request.getParameter("broadcastId")!=null) {
		broadcastId=request.getParameter("broadcastId");
	}
	if(userId==null || type==null || broadcastId==null){
		return;
	}
	
	

 ApplicationContext appCtx = (ApplicationContext) application.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
 Red5ProLive handler = (Red5ProLive)appCtx.getBean("web.handler");
 //token = handler.createDigest(type,broadcastId,userId);
%>

<%=token%>
