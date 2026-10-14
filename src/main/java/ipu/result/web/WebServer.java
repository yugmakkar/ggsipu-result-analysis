package ipu.result.web;

import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/** Starts the web application on an embedded Tomcat (Servlets + JSP). */
public class WebServer {

    private final Services services;
    private final int port;
    private Tomcat tomcat;

    public WebServer(Services services, int port) {
        this.services = services;
        this.port = port;
    }

    public void start() throws Exception {
        File docBase = resolveWebappDir();

        tomcat = new Tomcat();
        Path baseDir = Files.createTempDirectory("ipu-tomcat");
        tomcat.setBaseDir(baseDir.toString());
        tomcat.setPort(port);
        tomcat.getConnector(); // ensure a connector exists

        Context ctx = tomcat.addWebapp("", docBase.getAbsolutePath());
        ctx.getServletContext().setAttribute(Services.KEY, services);
        ctx.setSessionTimeout(30); // minutes

        addFilter(ctx, "securityHeaders", new SecurityHeadersFilter(), "/*");
        addFilter(ctx, "auth", new AuthFilter(), "/admin/*");

        addServlet(ctx, "home", new HomeServlet(), "");            // only "/"
        addServlet(ctx, "result", new ResultServlet(), "/result");
        addServlet(ctx, "toppers", new ToppersServlet(), "/toppers");
        addServlet(ctx, "login", new LoginServlet(), "/login");
        addServlet(ctx, "logout", new LogoutServlet(), "/logout");
        addServlet(ctx, "dashboard", new DashboardServlet(), "/admin");

        Wrapper upload = Tomcat.addServlet(ctx, "upload", new UploadServlet());
        upload.setMultipartConfigElement(new MultipartConfigElement(
                baseDir.toString(), services.config.uploadMaxBytes(),
                services.config.uploadMaxBytes() + 8192, 1024 * 1024));
        ctx.addServletMappingDecoded("/admin/upload", "upload");

        tomcat.start();
        System.out.println("Web server started on http://localhost:" + port + "  (Ctrl+C to stop)");
    }

    public void await() {
        tomcat.getServer().await();
    }

    public void stop() throws Exception {
        if (tomcat != null) { tomcat.stop(); tomcat.destroy(); }
    }

    private static void addServlet(Context ctx, String name, jakarta.servlet.Servlet servlet, String pattern) {
        Tomcat.addServlet(ctx, name, servlet);
        ctx.addServletMappingDecoded(pattern, name);
    }

    private static void addFilter(Context ctx, String name, Filter filter, String pattern) {
        FilterDef fd = new FilterDef();
        fd.setFilterName(name);
        fd.setFilter(filter);
        ctx.addFilterDef(fd);
        FilterMap fm = new FilterMap();
        fm.setFilterName(name);
        fm.addURLPattern(pattern);
        ctx.addFilterMap(fm);
    }

    /** Locates src/main/webapp (dev) or ./webapp (deployed next to the jar). */
    private static File resolveWebappDir() {
        for (String candidate : new String[]{"src/main/webapp", "webapp"}) {
            File f = new File(candidate);
            if (f.isDirectory()) return f;
        }
        throw new IllegalStateException(
                "webapp directory not found. Run from the project root, or place a 'webapp' folder next to the jar.");
    }
}
