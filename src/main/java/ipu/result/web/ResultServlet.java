package ipu.result.web;

import ipu.result.dto.Marksheet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Public result lookup by enrollment number (?enroll=...). */
public class ResultServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String enroll = req.getParameter("enroll");
        if (enroll != null && !enroll.isBlank()) {
            enroll = enroll.trim();
            Marksheet ms = services().toppers.marksheet(enroll); // PreparedStatement inside
            req.setAttribute("query", enroll);
            if (ms == null) {
                req.setAttribute("notFound", true);
            } else {
                req.setAttribute("ms", ms);
            }
        }
        view(req, res, "result.jsp");
    }
}
