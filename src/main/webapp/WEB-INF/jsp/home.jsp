<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Home"/>
<%@ include file="_header.jspf" %>

<h1>Result Analysis &amp; CGPA Calculator</h1>
<p class="lead">Semester results extracted from GGSIPU result PDFs, with SGPA and CGPA
   for every student.</p>

<div class="cards">
  <div class="card"><span class="num"><c:out value="${courses}"/></span><span>Courses</span></div>
  <div class="card"><span class="num"><c:out value="${students}"/></span><span>Students</span></div>
  <div class="card"><span class="num"><c:out value="${uploads}"/></span><span>Result uploads</span></div>
</div>

<section class="panel">
  <h2>Courses</h2>
  <p class="muted">Courses appear here once the admin uploads their result PDF.</p>
  <c:choose>
    <c:when test="${empty courseList}">
      <p class="muted">No results uploaded yet.</p>
    </c:when>
    <c:otherwise>
      <ul class="chips">
        <c:forEach var="course" items="${courseList}">
          <li><c:out value="${course.name}"/></li>
        </c:forEach>
      </ul>
    </c:otherwise>
  </c:choose>
</section>

<section class="panel">
  <h2>Check a result</h2>
  <form method="get" action="${pageContext.request.contextPath}/result" class="row">
    <input type="text" name="enroll" placeholder="Enrollment number" inputmode="numeric" required>
    <button type="submit">Search</button>
  </form>
  <c:choose>
    <c:when test="${resultsDeclared}">
      <p class="muted">Enter your enrollment number to check your result.</p>
    </c:when>
    <c:otherwise>
      <p class="muted">No result has been declared yet. Results appear here once the
         admin uploads the result PDF.</p>
    </c:otherwise>
  </c:choose>
</section>

<section class="panel">
  <h2>Toppers</h2>
  <c:choose>
    <c:when test="${toppersEnabled}">
      <p>
        <a href="${pageContext.request.contextPath}/toppers?type=overall">Overall</a> &middot;
        <a href="${pageContext.request.contextPath}/toppers?type=semester">Semester-wise</a> &middot;
        <a href="${pageContext.request.contextPath}/toppers?type=subject">Subject-wise</a>
      </p>
    </c:when>
    <c:otherwise>
      <p class="notice">Under development &mdash; overall, semester-wise and subject-wise
         toppers are planned for next semester.</p>
    </c:otherwise>
  </c:choose>
</section>

<%@ include file="_footer.jspf" %>
