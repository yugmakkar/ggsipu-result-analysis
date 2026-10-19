<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Result"/>
<%@ include file="_header.jspf" %>

<h1>Student result</h1>
<form method="get" action="${pageContext.request.contextPath}/result" class="row">
  <input type="text" name="enroll" value="<c:out value='${query}'/>" placeholder="Enrollment number" required>
  <input type="text" name="name" value="<c:out value='${queryName}'/>" placeholder="Student name"
         <c:if test="${not isAdmin}">required</c:if>>
  <button type="submit">Search</button>
</form>
<c:if test="${not isAdmin}">
  <p class="muted">Enter your enrollment number and your name exactly as on record.</p>
</c:if>

<c:if test="${notFound}">
  <p class="notice">No matching result. Check that the enrollment number and name
     are correct and match our records.</p>
</c:if>

<c:if test="${not empty ms}">
  <section class="panel">
    <h2><c:out value="${ms.name}"/></h2>
    <p class="meta">
      <c:out value="${ms.enrollmentNo}"/> &middot;
      <c:out value="${ms.collegeName}"/> &middot;
      <c:out value="${ms.programmeName}"/>
    </p>

    <c:choose>
      <%-- Internal-assessment view: internal marks only, no SGPA/grades yet. --%>
      <c:when test="${ms.internalOnly}">
        <c:forEach var="sem" items="${ms.semesters}">
          <h3>Semester <c:out value="${sem.semester}"/> &mdash; internal assessment</h3>
          <table>
            <thead><tr><th>Paper</th><th>Code</th><th>Subject</th><th>Internal</th></tr></thead>
            <tbody>
            <c:forEach var="r" items="${sem.rows}">
              <tr<c:if test="${r.absent}"> class="fail"</c:if>>
                <td><c:out value="${r.paperId}"/></td>
                <td><c:out value="${r.paperCode}"/></td>
                <td><c:out value="${r.subjectName}"/></td>
                <td><c:choose>
                  <c:when test="${r.absent}">AB</c:when>
                  <c:otherwise><c:out value="${r.internal}"/> / <c:out value="${r.internalMax}"/></c:otherwise>
                </c:choose></td>
              </tr>
            </c:forEach>
            </tbody>
          </table>
        </c:forEach>
        <p class="muted">Internal assessment marks only. SGPA, CGPA and grades are
           published after the end-semester results.</p>
      </c:when>

      <%-- End-semester view (dormant until external results are imported). --%>
      <c:otherwise>
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
      </c:otherwise>
    </c:choose>
  </section>
</c:if>

<%@ include file="_footer.jspf" %>
