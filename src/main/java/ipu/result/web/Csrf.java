package ipu.result.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;
import java.util.Base64;

/** Per-session CSRF token: issued on GET, required (and constant-time checked) on POST. */
public class Csrf {
    public static final String ATTR = "csrfToken";
    public static final String PARAM = "csrf";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Returns the session token, creating one if needed. */
    public static String token(HttpSession session) {
        String t = (String) session.getAttribute(ATTR);
        if (t == null) {
            byte[] b = new byte[32];
            RANDOM.nextBytes(b);
            t = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
            session.setAttribute(ATTR, t);
        }
        return t;
    }

    /** True if the request's token matches the session token. */
    public static boolean isValid(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        String expected = (String) session.getAttribute(ATTR);
        String given = req.getParameter(PARAM);
        return expected != null && given != null && constantTimeEquals(expected, given);
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] x = a.getBytes();
        byte[] y = b.getBytes();
        if (x.length != y.length) return false;
        int diff = 0;
        for (int i = 0; i < x.length; i++) diff |= x[i] ^ y[i];
        return diff == 0;
    }
}
