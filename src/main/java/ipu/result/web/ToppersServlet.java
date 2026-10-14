package ipu.result.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Public topper lists (?type=subject|semester|overall). */
public class ToppersServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        boolean enabled = services().config.toppersEnabled();
        req.setAttribute("toppersEnabled", enabled);

        String type = req.getParameter("type");
        if (type == null || type.isBlank()) type = "overall";

        // While the feature is under development we run NO topper query and expose
        // no data; the JSP shows a notice. This also covers direct deep links.
        if (enabled) {
            var t = services().toppers;
            switch (type) {
                case "subject"  -> req.setAttribute("subjectRows", t.subjectToppers());
                case "semester" -> req.setAttribute("rankRows", t.semesterToppers());
                default -> { type = "overall"; req.setAttribute("rankRows", t.overallToppers()); }
            }
        } else if (!type.equals("subject") && !type.equals("semester")) {
            type = "overall";
        }

        req.setAttribute("type", type);
        view(req, res, "toppers.jsp");
    }
}
