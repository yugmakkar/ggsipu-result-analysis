package ipu.result.web;

import ipu.result.dto.Marksheet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Result lookup. A public visitor must supply BOTH the enrollment number and the
 * student's name — since enrollment numbers are sequential, requiring the name
 * stops anyone from enumerating every student's marks. A logged-in admin may
 * look up by enrollment number alone (used by the dashboard's student links).
 *
 * To avoid an enumeration oracle, "no such student" and "name does not match"
 * return the exact same message.
 */
public class ResultServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String enroll = req.getParameter("enroll");
        String name = req.getParameter("name");
        if (enroll != null && !enroll.isBlank()) {
            enroll = enroll.trim();
            req.setAttribute("query", enroll);
            req.setAttribute("queryName", name == null ? "" : name.trim());

            Marksheet ms = services().toppers.marksheet(enroll); // PreparedStatement inside
            if (ms != null && (isAdmin(req) || nameMatches(name, ms.name()))) {
                req.setAttribute("ms", ms);
            } else {
                req.setAttribute("notFound", true);
            }
        }
        req.setAttribute("isAdmin", isAdmin(req));
        view(req, res, "result.jsp");
    }

    private static boolean isAdmin(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s != null && s.getAttribute(AuthFilter.SESSION_ADMIN) != null;
    }

    /** Case-insensitive, whitespace-normalised full-name match. */
    private static boolean nameMatches(String entered, String stored) {
        if (entered == null || stored == null) return false;
        return normalize(entered).equals(normalize(stored)) && !entered.isBlank();
    }

    private static String normalize(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
