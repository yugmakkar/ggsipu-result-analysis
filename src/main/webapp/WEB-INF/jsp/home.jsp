<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Home"/>
<%@ include file="_header.jspf" %>

<h1>Result Analysis &amp; CGPA Calculator</h1>
<p class="lead">Semester results extracted from GGSIPU result PDFs, with SGPA, CGPA and
   university-level toppers.</p>

<div class="cards">
  <div class="card"><span class="num"><c:out value="${colleges}"/></span><span>Colleges</span></div>
  <div class="card"><span class="num"><c:out value="${students}"/></span><span>Students</span></div>
  <div class="card"><span class="num"><c:out value="${uploads}"/></span><span>Result uploads</span></div>
</div>

<section class="panel">
  <h2>Check a result</h2>
  <form method="get" action="${pageContext.request.contextPath}/result" class="row">
    <input type="text" name="enroll" placeholder="Enrollment number" required>
    <button type="submit">Search</button>
  </form>
</section>

<section class="panel">
  <h2>Toppers</h2>
  <p>
    <a href="${pageContext.request.contextPath}/toppers?type=overall">Overall</a> &middot;
    <a href="${pageContext.request.contextPath}/toppers?type=college">College-wise</a> &middot;
    <a href="${pageContext.request.contextPath}/toppers?type=semester">Semester-wise</a> &middot;
    <a href="${pageContext.request.contextPath}/toppers?type=subject">Subject-wise</a>
  </p>
</section>

<%@ include file="_footer.jspf" %>
