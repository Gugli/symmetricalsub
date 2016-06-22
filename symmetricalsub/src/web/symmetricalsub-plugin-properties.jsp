<%--
  - Copyright (C) 2005-2008 Jive Software. All rights reserved.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
--%>

<%@ page import="java.util.*,
                 org.jivesoftware.openfire.user.*,
                 org.jivesoftware.openfire.XMPPServer,
				     com.nadeo.openfire.plugin.SymmetricalSubPlugin,
                 org.jivesoftware.util.*"
%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
   boolean save = request.getParameter("save") != null;
   boolean success = request.getParameter("success") != null;
   String mode = ParamUtils.getParameter(request, "mode");
  
   SymmetricalSubPlugin plugin = (SymmetricalSubPlugin) XMPPServer.getInstance().getPluginManager().getPlugin("symmetricalsub");
   if (save) {      
      plugin.setSymmetricalSubMode(mode);
      response.sendRedirect("symmetricalsub-plugin-properties.jsp?success=true");
      return;
   }
   mode = plugin.getSymmetricalSubMode();
%>

<html>
	<head>
	  <title>symmetricalsub Service Properties</title>
	  <meta name="pageID" content="symmetricalsub-plugin-properties"/>
   </head>
   <body>
<p>Use the form below to set the symmetricalsub service properties.</p>

<% if (success) { %>
	<div class="jive-success">
	<table cellpadding="0" cellspacing="0" border="0">
	<tbody>
	   <tr>
         <td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
         <td class="jive-icon-label">Service properties edited successfully.</td>
      </tr>
   </tbody>
   </table>
   </div>
   <br>
    
<% } %>

<form action="symmetricalsub-plugin-properties.jsp?save" name="notifyform" method="post">

<fieldset>
   <legend>symmetricalsub Service Settings</legend>
   <div>
   <table cellpadding="3" cellspacing="0" border="0" width="100%">
   <tbody>
      <tr>
         <td width="1%">
            <input type="radio" name="mode" value="<%= SymmetricalSubPlugin.DISABLED %>" id="rb01"
               <%= (mode.equals(SymmetricalSubPlugin.DISABLED) ? "checked" : "") %>>
         </td>
         <td width="99%">
            <label for="rb01"><strong>Disabled</strong></label> - Relationships can be asymmetrical (typical XMPP behaviour).
         </td>
      </tr>      
      <tr>
         <td width="1%">
            <input type="radio" name="mode" value="<%= SymmetricalSubPlugin.LOCAL %>" id="rb02"
               <%= (mode.equals(SymmetricalSubPlugin.LOCAL) ? "checked" : "") %>>
         </td>
         <td width="99%">
            <label for="rb02"><strong>Local</strong></label> - Local subscriptions will be symmetrical.
         </td>
      </tr>
      <tr>
         <td width="1%">
            <input type="radio" name="mode" value="<%= SymmetricalSubPlugin.ALL %>" id="rb03"
               <%= (mode.equals(SymmetricalSubPlugin.ALL) ? "checked" : "") %>>
         </td>
         <td width="99%">
            <label for="rb02"><strong>All</strong></label> - Try to apply symmetrical subscriptions to anyone. Does not make sense if other domains are not behaving this way
         </td>
      </tr>
   </tbody>
   </table>
   </div>
   
	<br>
   <input type="submit" value="Save Settings">
</fieldset>

<br><br>

</form>

</body>
</html>