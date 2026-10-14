<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="All students"/>
<%@ include file="_header.jspf" %>

<div class="bar">
  <h1>All students</h1>
  <a href="${pageContext.request.contextPath}/admin">&larr; Dashboard</a>
</div>

<form method="get" action="${pageContext.request.contextPath}/admin/students" class="row">
  <select name="college">
    <option value="">All colleges</option>
    <c:forEach var="col" items="${colleges}">
      <option value="<c:out value='${col.code}'/>" ${fCollege eq col.code ? 'selected' : ''}>
        <c:out value="${col.code}"/> &mdash; <c:out value="${col.name}"/>
      </option>
    </c:forEach>
  </select>
  <select name="programme">
    <option value="">All programmes</option>
    <c:forEach var="p" items="${programmes}">
      <option value="<c:out value='${p.code}'/>" ${fProgramme eq p.code ? 'selected' : ''}>
        <c:out value="${p.code}"/> &mdash; <c:out value="${p.name}"/>
      </option>
    </c:forEach>
  </select>
  <button type="submit">Filter</button>
</form>

<p class="muted"><c:out value="${students.size()}"/> student(s)</p>
<table>
  <thead><tr><th>Enrollment</th><th>Name</th><th>College</th><th>Programme</th><th>CGPA</th><th>Status</th></tr></thead>
  <tbody>
  <c:forEach var="s" items="${students}">
    <tr<c:if test="${s.hasBacklog}"> class="fail"</c:if>>
      <td><a href="${pageContext.request.contextPath}/result?enroll=<c:out value='${s.enrollmentNo}'/>"><c:out value="${s.enrollmentNo}"/></a></td>
      <td><c:out value="${s.name}"/></td>
      <td><c:out value="${s.collegeCode}"/></td>
      <td><c:out value="${s.programmeCode}"/></td>
      <td><c:out value="${s.cgpa}"/></td>
      <td><c:choose><c:when test="${s.hasBacklog}">Backlog</c:when><c:otherwise>Clear</c:otherwise></c:choose></td>
    </tr>
  </c:forEach>
  <c:if test="${empty students}"><tr><td colspan="6" class="muted">No students.</td></tr></c:if>
  </tbody>
</table>

<%@ include file="_footer.jspf" %>
