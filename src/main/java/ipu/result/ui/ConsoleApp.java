package ipu.result.ui;

import ipu.result.config.AppConfig;
import ipu.result.dao.Db;
import ipu.result.dto.Marksheet;
import ipu.result.dto.Toppers;
import ipu.result.service.ImportService;
import ipu.result.service.TopperService;

import java.io.File;
import java.util.List;
import java.util.Scanner;

/**
 * Semester 1 deliverable: a menu-driven console front-end over the same engine
 * the web layer uses. No database or web server setup required - it runs on the
 * embedded H2 database by default.
 */
public class ConsoleApp {

    private final Db db;
    private final ImportService importService;
    private final TopperService toppers;
    private final Scanner in = new Scanner(System.in);

    public ConsoleApp(AppConfig cfg) {
        this.db = new Db(cfg.dbUrl(), cfg.dbUser(), cfg.dbPassword());
        db.initSchema();
        this.importService = new ImportService(db);
        this.toppers = new TopperService(db);
    }

    public void run() {
        System.out.println("GGSIPU Result Analysis - console");
        boolean go = true;
        while (go) {
            printMenu();
            switch (in.nextLine().trim()) {
                case "1" -> importPdf();
                case "2" -> lookupStudent();
                case "3" -> showToppers();
                case "4" -> showStats();
                case "0" -> go = false;
                default  -> System.out.println("Unknown option.");
            }
        }
        System.out.println("Bye.");
    }

    private void printMenu() {
        System.out.println("""

                1. Import a result PDF
                2. Look up a student's result
                3. Toppers
                4. Statistics
                0. Exit""");
        System.out.print("> ");
    }

    private void importPdf() {
        System.out.print("Path to PDF: ");
        String path = in.nextLine().trim();
        File pdf = new File(path);
        if (!pdf.isFile()) { System.out.println("No such file."); return; }
        try {
            var s = importService.importPdf(pdf, pdf.getName(), pdf.getName());
            System.out.printf("Imported: %d students, %d marks, %d semester results (session %s).%n",
                    s.students(), s.marks(), s.semesterResults(), s.session());
        } catch (Exception e) {
            System.out.println("Import failed: " + e.getMessage());
        }
    }

    private void lookupStudent() {
        System.out.print("Enrollment no: ");
        String enroll = in.nextLine().trim();
        Marksheet ms = toppers.marksheet(enroll);
        if (ms == null) { System.out.println("Student not found."); return; }
        System.out.printf("%n%s  (%s)%n", ms.name(), ms.enrollmentNo());
        System.out.printf("%s | %s%n", nz(ms.collegeName()), nz(ms.programmeName()));
        if (ms.internalOnly()) {
            for (Marksheet.SemesterBlock b : ms.semesters()) {
                System.out.printf("%n Semester %d   internal assessment%n", b.semester());
                System.out.println(" ---------------------------------------------");
                for (Marksheet.MarkRow r : b.rows()) {
                    String mark = r.absent() ? "AB" : r.internal() + "/" + r.internalMax();
                    System.out.printf("  %-10s %-28s %s%n",
                            nz(r.paperCode()), trim(r.subjectName(), 28), mark);
                }
            }
            System.out.println("\n (SGPA/CGPA appear after end-semester results.)");
            return;
        }
        for (Marksheet.SemesterBlock b : ms.semesters()) {
            System.out.printf("%n Semester %d   SGPA %.2f%s%n", b.semester(), b.sgpa(),
                    b.hasBacklog() ? "  (backlog)" : "");
            System.out.println(" ---------------------------------------------");
            for (Marksheet.MarkRow r : b.rows()) {
                System.out.printf("  %-28s cr%d  %-4s %s%n",
                        trim(r.subjectName(), 28), r.credits(),
                        r.absent() ? "AB" : String.valueOf(r.total()), nz(r.grade()));
            }
        }
        System.out.printf("%n CGPA: %.2f%s%n", ms.cgpa(), ms.hasBacklog() ? "  (has backlog)" : "");
    }

    private void showToppers() {
        System.out.print("Type [subject/college/semester/overall]: ");
        switch (in.nextLine().trim().toLowerCase()) {
            case "subject" -> {
                for (Toppers.SubjectTopper t : toppers.subjectToppers())
                    System.out.printf("  %-28s %-18s %d (%s)%n",
                            trim(t.subjectName(), 28), t.studentName(), t.totalMarks(), t.grade());
            }
            case "college" -> printRank(toppers.collegeToppers());
            case "semester" -> printRank(toppers.semesterToppers());
            case "overall" -> printRank(toppers.overallToppers());
            default -> System.out.println("Unknown type.");
        }
    }

    private void printRank(List<Toppers.RankTopper> rows) {
        if (rows.isEmpty()) { System.out.println("  (no eligible students)"); return; }
        for (Toppers.RankTopper t : rows)
            System.out.printf("  %-14s %-18s %.2f%n", t.scope(), t.studentName(), t.value());
    }

    private void showStats() {
        System.out.printf("  Colleges: %d | Students: %d | Uploads: %d%n",
                toppers.collegeCount(), toppers.studentCount(), toppers.uploadCount());
    }

    private static String nz(String s) { return s == null ? "-" : s; }
    private static String trim(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }
}
