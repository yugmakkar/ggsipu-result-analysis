package ipu.result.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Public topper lists (?type=subject|college|semester|overall). */
public class ToppersServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String type = req.getParameter("type");
        if (type == null || type.isBlank()) type = "overall";
        var t = services().toppers;
        switch (type) {
            case "subject"  -> req.setAttribute("subjectRows", t.subjectToppers());
            case "college"  -> req.setAttribute("rankRows", t.collegeToppers());
            case "semester" -> req.setAttribute("rankRows", t.semesterToppers());
            default -> { type = "overall"; req.setAttribute("rankRows", t.overallToppers()); }
        }
        req.setAttribute("type", type);
        view(req, res, "toppers.jsp");
    }
}
