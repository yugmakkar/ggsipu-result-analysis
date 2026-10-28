package ipu.result.web;

import ipu.result.dto.Marksheet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Result lookup. A student checks their result with their enrollment number
 * alone &mdash; no name is required.
 *
 * <p>Three outcomes are distinguished so the student always sees a clear message:
 * <ul>
 *   <li><b>declared</b> &mdash; an end-semester result exists, so the full
 *       marksheet (marks, SGPA, CGPA) is shown;</li>
 *   <li><b>not declared yet</b> &mdash; the student exists but no end-semester
 *       result has been published for them (e.g. only internal marks are on
 *       record, or nothing has been uploaded yet);</li>
 *   <li><b>no record</b> &mdash; the enrollment number is not in the system.</li>
 * </ul>
 * A signed-in admin always sees whatever is on record (including internal-only
 * marks) so uploads can be verified.
 */
public class ResultServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        boolean isAdmin = isAdmin(req);
        req.setAttribute("isAdmin", isAdmin);

        String enroll = req.getParameter("enroll");
        if (enroll != null && !enroll.isBlank()) {
            enroll = enroll.trim();
            req.setAttribute("query", enroll);

            if (!enroll.matches("\\d{6,20}")) {
                req.setAttribute("invalid", true);          // not an enrollment number at all
            } else {
                Marksheet ms = services().toppers.marksheet(enroll); // PreparedStatement inside
                if (ms == null) {
                    req.setAttribute("noRecord", true);
                } else if (ms.isInternalOnly() && !isAdmin) {
                    // Only internal marks on record: the end-semester result is not out yet.
                    req.setAttribute("notDeclared", true);
                } else {
                    req.setAttribute("ms", ms);
                }
            }
        }
        view(req, res, "result.jsp");
    }

    private static boolean isAdmin(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s != null && s.getAttribute(AuthFilter.SESSION_ADMIN) != null;
    }
}
