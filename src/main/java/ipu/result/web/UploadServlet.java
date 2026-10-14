package ipu.result.web;

import ipu.result.service.ImportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Handles the admin PDF upload. Defences: CSRF token, size limit, PDF magic-byte
 * check, and a server-generated storage name (so a crafted filename cannot cause
 * path traversal). The import runs in a single transaction; on any failure the
 * stored file is removed and nothing is committed.
 */
public class UploadServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!Csrf.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        var services = services();
        Path dir = Path.of(services.config.uploadDir());
        Files.createDirectories(dir);

        Part part;
        try {
            part = req.getPart("pdf");
        } catch (Exception e) { // thrown when the request exceeds the size limit
            finish(req, res, null, "Upload rejected: file too large or malformed.");
            return;
        }
        if (part == null || part.getSize() == 0) {
            finish(req, res, null, "Please choose a PDF file.");
            return;
        }
        if (part.getSize() > services.config.uploadMaxBytes()) {
            finish(req, res, null, "File exceeds the size limit.");
            return;
        }

        // server-generated name only; the submitted name is used for display, never as a path
        String originalName = sanitizeForDisplay(part.getSubmittedFileName());
        String storedName = UUID.randomUUID() + ".pdf";
        Path target = dir.resolve(storedName);

        try (InputStream in = part.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        if (!looksLikePdf(target)) {
            Files.deleteIfExists(target);
            finish(req, res, null, "That file is not a PDF.");
            return;
        }

        try {
            ImportService.ImportSummary s =
                    services.importService.importPdf(target.toFile(), originalName, storedName);
            String msg = String.format("Imported %s: %d students, %d marks, %d semester results (session %s).",
                    originalName, s.students(), s.marks(), s.semesterResults(), s.session());
            finish(req, res, msg, null);
        } catch (Exception e) {
            Files.deleteIfExists(target); // roll back the stored file too
            finish(req, res, null, "Could not process the PDF: " + e.getMessage());
        }
    }

    private void finish(HttpServletRequest req, HttpServletResponse res, String ok, String error)
            throws ServletException, IOException {
        if (ok != null) req.setAttribute("uploadOk", ok);
        if (error != null) req.setAttribute("uploadError", error);
        var t = services().toppers;
        req.setAttribute("colleges", t.collegeCount());
        req.setAttribute("students", t.studentCount());
        req.setAttribute("uploads", t.uploadCount());
        req.setAttribute("recent", t.recentUploads(10));
        view(req, res, "dashboard.jsp");
    }

    private static boolean looksLikePdf(Path file) throws IOException {
        byte[] head = new byte[5];
        try (InputStream in = Files.newInputStream(file)) {
            int n = in.read(head);
            return n == 5 && head[0] == '%' && head[1] == 'P' && head[2] == 'D'
                    && head[3] == 'F' && head[4] == '-';
        }
    }

    private static String sanitizeForDisplay(String name) {
        if (name == null || name.isBlank()) return "upload.pdf";
        // keep only the base name, strip anything that is not a safe character
        String base = name.replaceAll(".*[/\\\\]", "");
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return base.length() > 120 ? base.substring(0, 120) : base;
    }
}
