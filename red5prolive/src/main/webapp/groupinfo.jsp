<%@ page import="com.red5pro.webrtc.group.BasicRTCCompositor"%>
<% 
    String group="live/group01";
    if (request.getParameter("group") != null) {
        group = request.getParameter("group");
     }
     group = BasicRTCCompositor.getInfo(group);
%>
<%=group %>
