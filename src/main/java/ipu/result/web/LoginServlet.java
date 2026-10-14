package ipu.result.web;

import ipu.result.dao.AdminDAO;
import ipu.result.service.PasswordHasher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;

/** Admin login. Verifies PBKDF2 hash, throttles failures, rotates the session id. */
public class LoginServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        view(req, res, "login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!Csrf.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || password == null) {
            fail(req, res, "Enter username and password.");
            return;
        }
        username = username.trim();

        AdminDAO dao = services().adminDAO;
        Optional<AdminDAO.AdminAuth> found = dao.find(username);
        if (found.isEmpty()) {
            fail(req, res, "Invalid credentials.");
            return;
        }
        AdminDAO.AdminAuth admin = found.get();
        if (admin.isLocked()) {
            fail(req, res, "Account temporarily locked. Try again later.");
            return;
        }

        boolean ok = PasswordHasher.verify(password.toCharArray(), admin.passwordHash());
        if (!ok) {
            dao.recordFailedLogin(username);
            fail(req, res, "Invalid credentials.");
            return;
        }

        dao.resetFailedLogins(username);
        // prevent session fixation: new session id on privilege change
        req.changeSessionId();
        HttpSession session = req.getSession(true);
        session.setAttribute(AuthFilter.SESSION_ADMIN, username);
        res.sendRedirect(req.getContextPath() + "/admin");
    }

    private void fail(HttpServletRequest req, HttpServletResponse res, String msg)
            throws ServletException, IOException {
        req.setAttribute("error", msg);
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        view(req, res, "login.jsp");
    }
}
