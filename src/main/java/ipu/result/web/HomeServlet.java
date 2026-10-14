package ipu.result.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Public landing page with university-level counts. */
public class HomeServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        var t = services().toppers;
        req.setAttribute("courseList", t.listProgrammes());
        req.setAttribute("courses", t.courseCount());
        req.setAttribute("students", t.studentCount());
        req.setAttribute("uploads", t.uploadCount());
        req.setAttribute("toppersEnabled", services().config.toppersEnabled());
        view(req, res, "home.jsp");
    }
}
