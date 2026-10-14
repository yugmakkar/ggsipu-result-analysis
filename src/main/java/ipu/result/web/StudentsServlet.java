package ipu.result.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Admin-only: every student with CGPA and backlog, filterable by college/programme. */
public class StudentsServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        var t = services().toppers;
        String college = req.getParameter("college");
        String programme = req.getParameter("programme");
        req.setAttribute("colleges", t.listColleges());
        req.setAttribute("programmes", t.listProgrammes());
        req.setAttribute("students", t.listStudents(college, programme));
        req.setAttribute("fCollege", college == null ? "" : college);
        req.setAttribute("fProgramme", programme == null ? "" : programme);
        view(req, res, "students.jsp");
    }
}
