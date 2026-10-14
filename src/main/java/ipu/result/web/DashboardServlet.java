package ipu.result.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Admin dashboard: counts, recent uploads, and the upload form. */
public class DashboardServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        var t = services().toppers;
        req.setAttribute("courses", t.courseCount());
        req.setAttribute("students", t.studentCount());
        req.setAttribute("uploads", t.uploadCount());
        req.setAttribute("recent", t.recentUploads(10));
        view(req, res, "dashboard.jsp");
    }
}
