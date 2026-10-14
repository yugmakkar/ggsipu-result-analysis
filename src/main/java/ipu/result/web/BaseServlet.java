package ipu.result.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

abstract class BaseServlet extends HttpServlet {

    protected Services services() {
        return Services.from(getServletContext());
    }

    protected void view(HttpServletRequest req, HttpServletResponse res, String jsp)
            throws ServletException, IOException {
        // expose a CSRF token to every rendered page for use in forms
        req.setAttribute("csrfToken", Csrf.token(req.getSession(true)));
        req.getRequestDispatcher("/WEB-INF/jsp/" + jsp).forward(req, res);
    }
}
