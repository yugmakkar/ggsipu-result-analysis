package ipu.result;

import ipu.result.config.AppConfig;
import ipu.result.dao.AdminDAO;
import ipu.result.dao.Db;
import ipu.result.parser.FixturePdfGenerator;
import ipu.result.parser.InternalFixtureGenerator;
import ipu.result.service.ImportService;
import ipu.result.service.PasswordHasher;
import ipu.result.ui.ConsoleApp;
import ipu.result.web.Services;
import ipu.result.web.WebServer;

import java.io.Console;
import java.io.File;

/**
 * Single entry point. Sub-commands:
 *   web                       start the web application
 *   console                   run the Semester 1 console menu
 *   import <file.pdf>         import one PDF into the database
 *   create-admin <username>   create an admin (password via prompt or ADMIN_PASSWORD env)
 *   gen-fixture [out.pdf]     write the sample result PDF
 */
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) { usage(); return; }
        AppConfig cfg = new AppConfig();

        switch (args[0]) {
            case "web"        -> runWeb(cfg);
            case "console"    -> new ConsoleApp(cfg).run();
            case "import"     -> runImport(cfg, args);
            case "create-admin" -> createAdmin(cfg, args);
            case "gen-fixture"  -> {
                // A synthetic Internal Assessment sheet (the Semester-1 format).
                File out = new File(args.length > 1 ? args[1] : "sample-internal.pdf");
                String course = args.length > 2 ? args[2].toLowerCase() : "bca";
                var spec = course.equals("mca")
                        ? InternalFixtureGenerator.mca() : InternalFixtureGenerator.bca();
                new InternalFixtureGenerator().generate(out, spec);
                System.out.println("Wrote " + out.getAbsolutePath() + " (" + spec.programmeShort() + ")");
            }
            case "gen-fixture-legacy" -> {
                // The end-semester (Sem-2) sample, kept for the dormant results path.
                File out = new File(args.length > 1 ? args[1] : "sample-result.pdf");
                new FixturePdfGenerator().generate(out);
                System.out.println("Wrote " + out.getAbsolutePath());
            }
            default -> usage();
        }
    }

    private static void runWeb(AppConfig cfg) throws Exception {
        Db db = new Db(cfg.dbUrl(), cfg.dbUser(), cfg.dbPassword());
        db.initSchema();
        Services services = new Services(cfg, db);
        if (services.adminDAO.find("admin").isEmpty() && !hasAnyAdmin(db)) {
            System.out.println("NOTE: no admin account exists yet. Create one with:");
            System.out.println("      java -jar result-analysis.jar create-admin <username>");
        }
        WebServer server = new WebServer(services, cfg.webPort());
        server.start();
        server.await();
    }

    private static void runImport(AppConfig cfg, String[] args) throws Exception {
        if (args.length < 2) { System.out.println("usage: import <file.pdf>"); return; }
        File pdf = new File(args[1]);
        if (!pdf.isFile()) { System.out.println("No such file: " + pdf); return; }
        Db db = new Db(cfg.dbUrl(), cfg.dbUser(), cfg.dbPassword());
        db.initSchema();
        var s = new ImportService(db).importPdf(pdf, pdf.getName(), pdf.getName());
        System.out.printf("Imported %d students, %d marks (session %s).%n",
                s.students(), s.marks(), s.session());
    }

    private static void createAdmin(AppConfig cfg, String[] args) {
        if (args.length < 2) { System.out.println("usage: create-admin <username>"); return; }
        String username = args[1].trim();
        Db db = new Db(cfg.dbUrl(), cfg.dbUser(), cfg.dbPassword());
        db.initSchema();
        AdminDAO dao = new AdminDAO(db);
        if (dao.exists(username)) { System.out.println("Admin already exists: " + username); return; }

        char[] password = readPassword();
        if (password == null || password.length < 8) {
            System.out.println("Password must be at least 8 characters. Aborted.");
            return;
        }
        dao.create(username, PasswordHasher.hash(password));
        java.util.Arrays.fill(password, '\0');
        System.out.println("Created admin: " + username);
    }

    /** Reads the password from the console (not echoed), or from ADMIN_PASSWORD. */
    private static char[] readPassword() {
        String env = System.getenv("ADMIN_PASSWORD");
        if (env != null && !env.isBlank()) return env.toCharArray();
        Console console = System.console();
        if (console == null) {
            System.out.println("No console available; set ADMIN_PASSWORD env var instead.");
            return null;
        }
        return console.readPassword("New admin password: ");
    }

    private static boolean hasAnyAdmin(Db db) {
        try (var c = db.open(); var st = c.createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM admin")) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) { return false; }
    }

    private static void usage() {
        System.out.println("""
                GGSIPU Result Analysis
                usage: java -jar result-analysis.jar <command>
                  web                      start the web application
                  console                  Semester 1 console menu
                  import <file.pdf>        import one result PDF
                  create-admin <username>  create an admin account
                  gen-fixture [out.pdf] [bca|mca]  write a sample Internal Assessment PDF
                  gen-fixture-legacy [out.pdf]     write the end-semester sample PDF""");
    }
}
