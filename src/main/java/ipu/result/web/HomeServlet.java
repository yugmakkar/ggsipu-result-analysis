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
        req.setAttribute("colleges", t.collegeCount());
        req.setAttribute("students", t.studentCount());
        req.setAttribute("uploads", t.uploadCount());
        view(req, res, "home.jsp");
    }
}
