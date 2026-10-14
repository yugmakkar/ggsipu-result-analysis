<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Admin"/>
<%@ include file="_header.jspf" %>

<div class="bar">
  <h1>Admin dashboard</h1>
  <div class="row">
    <a href="${pageContext.request.contextPath}/admin/students">All students</a>
  <form method="post" action="${pageContext.request.contextPath}/logout">
    <input type="hidden" name="csrf" value="<c:out value='${csrfToken}'/>">
    <button type="submit" class="link">Log out</button>
  </form>
  </div>
</div>

<c:if test="${not empty uploadOk}"><p class="ok"><c:out value="${uploadOk}"/></p></c:if>
<c:if test="${not empty uploadError}"><p class="notice"><c:out value="${uploadError}"/></p></c:if>

<div class="cards">
  <div class="card"><span class="num"><c:out value="${colleges}"/></span><span>Colleges</span></div>
  <div class="card"><span class="num"><c:out value="${students}"/></span><span>Students</span></div>
  <div class="card"><span class="num"><c:out value="${uploads}"/></span><span>Uploads</span></div>
</div>

<section class="panel">
  <h2>Upload a result PDF</h2>
  <form method="post" action="${pageContext.request.contextPath}/admin/upload"
        enctype="multipart/form-data" class="row">
    <input type="hidden" name="csrf" value="<c:out value='${csrfToken}'/>">
    <input type="file" name="pdf" accept="application/pdf" required>
    <button type="submit">Upload &amp; import</button>
  </form>
  <p class="muted">One PDF may contain many colleges, programmes and semesters. Re-uploading
     the same results replaces them rather than duplicating.</p>
</section>

<section class="panel">
  <h2>Recent uploads</h2>
  <table>
    <thead><tr><th>#</th><th>File</th><th>Session</th><th>Uploaded</th><th>Rows</th></tr></thead>
    <tbody>
    <c:forEach var="u" items="${recent}">
      <tr>
        <td><c:out value="${u.id}"/></td>
        <td><c:out value="${u.fileName}"/></td>
        <td><c:out value="${u.session}"/></td>
        <td><c:out value="${u.uploadedOn}"/></td>
        <td><c:out value="${u.rowCount}"/></td>
      </tr>
    </c:forEach>
    <c:if test="${empty recent}"><tr><td colspan="5" class="muted">No uploads yet.</td></tr></c:if>
    </tbody>
  </table>
</section>

<%@ include file="_footer.jspf" %>
