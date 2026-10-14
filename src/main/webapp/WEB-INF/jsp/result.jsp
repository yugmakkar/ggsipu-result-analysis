<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Result"/>
<%@ include file="_header.jspf" %>

<h1>Student result</h1>
<form method="get" action="${pageContext.request.contextPath}/result" class="row">
  <input type="text" name="enroll" value="<c:out value='${query}'/>" placeholder="Enrollment number" required>
  <button type="submit">Search</button>
</form>

<c:if test="${notFound}">
  <p class="notice">No student found for enrollment number "<c:out value='${query}'/>".</p>
</c:if>

<c:if test="${not empty ms}">
  <section class="panel">
    <h2><c:out value="${ms.name}"/></h2>
    <p class="meta">
      <c:out value="${ms.enrollmentNo}"/> &middot;
      <c:out value="${ms.collegeName}"/> &middot;
      <c:out value="${ms.programmeName}"/>
    </p>

    <c:forEach var="sem" items="${ms.semesters}">
      <h3>Semester <c:out value="${sem.semester}"/>
        &mdash; SGPA <c:out value="${sem.sgpa}"/>
        <c:if test="${sem.hasBacklog}"><span class="tag">backlog</span></c:if>
      </h3>
      <table>
        <thead><tr><th>Paper</th><th>Subject</th><th>Credits</th><th>Internal</th><th>External</th><th>Total</th><th>Grade</th></tr></thead>
        <tbody>
        <c:forEach var="r" items="${sem.rows}">
          <tr<c:if test="${r.absent or r.grade eq 'F'}"> class="fail"</c:if>>
            <td><c:out value="${r.paperId}"/></td>
            <td><c:out value="${r.subjectName}"/></td>
            <td><c:out value="${r.credits}"/></td>
            <td><c:out value="${r.internal}"/></td>
            <td><c:choose><c:when test="${r.absent}">AB</c:when><c:otherwise><c:out value="${r.external}"/></c:otherwise></c:choose></td>
            <td><c:choose><c:when test="${r.absent}">AB</c:when><c:otherwise><c:out value="${r.total}"/></c:otherwise></c:choose></td>
            <td><c:out value="${r.grade}"/></td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </c:forEach>

    <p class="cgpa">CGPA: <strong><c:out value="${ms.cgpa}"/></strong>
      <c:if test="${ms.hasBacklog}"><span class="tag">has backlog</span></c:if>
    </p>
  </section>
</c:if>

<%@ include file="_footer.jspf" %>
