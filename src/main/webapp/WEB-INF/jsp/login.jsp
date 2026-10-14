<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Admin login"/>
<%@ include file="_header.jspf" %>

<h1>Admin login</h1>
<c:if test="${not empty error}">
  <p class="notice"><c:out value="${error}"/></p>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/login" class="stack">
  <input type="hidden" name="csrf" value="<c:out value='${csrfToken}'/>">
  <label>Username <input type="text" name="username" required autofocus></label>
  <label>Password <input type="password" name="password" required></label>
  <button type="submit">Sign in</button>
</form>

<%@ include file="_footer.jspf" %>
