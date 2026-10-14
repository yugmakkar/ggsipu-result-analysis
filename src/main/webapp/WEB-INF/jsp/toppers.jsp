<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Toppers"/>
<%@ include file="_header.jspf" %>

<h1>Toppers</h1>
<nav class="tabs">
  <a class="${type eq 'overall'  ? 'on' : ''}" href="?type=overall">Overall</a>
  <a class="${type eq 'college'  ? 'on' : ''}" href="?type=college">College-wise</a>
  <a class="${type eq 'semester' ? 'on' : ''}" href="?type=semester">Semester-wise</a>
  <a class="${type eq 'subject'  ? 'on' : ''}" href="?type=subject">Subject-wise</a>
</nav>

<c:choose>
  <c:when test="${type eq 'subject'}">
    <table>
      <thead><tr><th>Paper</th><th>Subject</th><th>Topper</th><th>College</th><th>Marks</th><th>Grade</th></tr></thead>
      <tbody>
      <c:forEach var="t" items="${subjectRows}">
        <tr>
          <td><c:out value="${t.paperId}"/></td>
          <td><c:out value="${t.subjectName}"/></td>
          <td><c:out value="${t.studentName}"/></td>
          <td><c:out value="${t.collegeCode}"/></td>
          <td><c:out value="${t.totalMarks}"/></td>
          <td><c:out value="${t.grade}"/></td>
        </tr>
      </c:forEach>
      <c:if test="${empty subjectRows}"><tr><td colspan="6" class="muted">No data yet.</td></tr></c:if>
      </tbody>
    </table>
  </c:when>
  <c:otherwise>
    <table>
      <thead><tr><th>Scope</th><th>Topper</th><th>College</th><th>Programme</th>
        <th>${type eq 'semester' ? 'SGPA' : 'CGPA'}</th></tr></thead>
      <tbody>
      <c:forEach var="t" items="${rankRows}">
        <tr>
          <td><c:out value="${t.scope}"/></td>
          <td><c:out value="${t.studentName}"/></td>
          <td><c:out value="${t.collegeCode}"/></td>
          <td><c:out value="${t.programmeCode}"/></td>
          <td><c:out value="${t.value}"/></td>
        </tr>
      </c:forEach>
      <c:if test="${empty rankRows}"><tr><td colspan="5" class="muted">No eligible students yet.</td></tr></c:if>
      </tbody>
    </table>
    <p class="muted">Students with a backlog are excluded from SGPA/CGPA toppers.</p>
  </c:otherwise>
</c:choose>

<%@ include file="_footer.jspf" %>
