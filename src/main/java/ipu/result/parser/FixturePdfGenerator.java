package ipu.result.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a sample GGSIPU-style result PDF in the documented line grammar
 * (see docs/pdf-format.md). It deliberately holds several colleges, programmes
 * and two semesters in one file - the "all courses in one upload" case - plus
 * one absentee and one failing student, so the whole pipeline and the topper
 * logic can be exercised end to end.
 *
 * This stands in for a real gazette only in layout; the numbers are invented.
 */
public class FixturePdfGenerator {

    private record Paper(String id, String code, String name, int credits) {}
    private record Stu(String enroll, String name, int[] totals) {} // -1 == absent
    private record Group(String college, String collegeName,
                         String prog, String progName, int sem,
                         Paper[] papers, Stu[] students) {}

    private static final Paper[] CSE_S1 = {
            new Paper("99101", "MA101", "Mathematics-I", 4),
            new Paper("99102", "CS103", "Programming Fundamentals", 4),
            new Paper("99103", "CS105", "Digital Logic", 3),
    };
    private static final Paper[] CSE_S2 = {
            new Paper("99111", "MA102", "Mathematics-II", 4),
            new Paper("99112", "CS104", "Data Structures", 4),
            new Paper("99113", "CS106", "Computer Organization", 3),
    };
    private static final Paper[] IT_S1 = {
            new Paper("99201", "MA101", "Mathematics-I", 4),
            new Paper("99202", "IT103", "Web Fundamentals", 4),
            new Paper("99203", "IT105", "Electronics", 3),
    };
    private static final Paper[] IT_S2 = {
            new Paper("99211", "MA102", "Mathematics-II", 4),
            new Paper("99212", "IT104", "Object Oriented Programming", 4),
            new Paper("99213", "IT106", "Database Systems", 3),
    };

    private static final List<Group> GROUPS = List.of(
        new Group("101", "University School of Information Technology", "020", "B.Tech Computer Science", 1, CSE_S1, new Stu[]{
            new Stu("00101012024", "AARAV SHARMA", new int[]{90, 88, 80}),
            new Stu("00201012024", "DIYA PATEL",   new int[]{75, 70, 65}),
            new Stu("00301012024", "KABIR SINGH",  new int[]{30, 55, 48}), // F in paper 1
        }),
        new Group("101", "University School of Information Technology", "020", "B.Tech Computer Science", 2, CSE_S2, new Stu[]{
            new Stu("00101012024", "AARAV SHARMA", new int[]{85, 90, 78}),
            new Stu("00201012024", "DIYA PATEL",   new int[]{72, 68, 60}),
            new Stu("00301012024", "KABIR SINGH",  new int[]{50, 52, 45}),
        }),
        new Group("101", "University School of Information Technology", "021", "B.Tech Information Technology", 1, IT_S1, new Stu[]{
            new Stu("00401012024", "MEERA NAIR", new int[]{95, 92, 85}),
            new Stu("00501012024", "ROHAN DAS",  new int[]{60, 55, 50}),
        }),
        new Group("101", "University School of Information Technology", "021", "B.Tech Information Technology", 2, IT_S2, new Stu[]{
            new Stu("00401012024", "MEERA NAIR", new int[]{96, 94, 90}),
            new Stu("00501012024", "ROHAN DAS",  new int[]{58, 56, 52}),
        }),
        new Group("164", "Maharaja Agrasen Institute of Technology", "020", "B.Tech Computer Science", 1, CSE_S1, new Stu[]{
            new Stu("01101012024", "ANANYA RAO",   new int[]{85, 82, 78}),
            new Stu("01201012024", "VIVAAN GUPTA", new int[]{70, 65, -1}), // absent in paper 3
            new Stu("01301012024", "ISHA VERMA",   new int[]{70, 68, 72}),
        }),
        new Group("164", "Maharaja Agrasen Institute of Technology", "020", "B.Tech Computer Science", 2, CSE_S2, new Stu[]{
            new Stu("01101012024", "ANANYA RAO",   new int[]{88, 80, 76}),
            new Stu("01201012024", "VIVAAN GUPTA", new int[]{72, 60, 55}),
            new Stu("01301012024", "ISHA VERMA",   new int[]{66, 70, 74}),
        })
    );

    /** Builds the pipe-delimited script that both the PDF and the grammar use. */
    public static List<String> buildScript() {
        List<String> lines = new ArrayList<>();
        lines.add("GURU GOBIND SINGH INDRAPRASTHA UNIVERSITY");
        lines.add("CONSOLIDATED RESULT GAZETTE");
        lines.add("SESSION|December 2024");
        lines.add("");
        for (Group g : GROUPS) {
            lines.add("COLLEGE|" + g.college() + "|" + g.collegeName());
            lines.add("PROGRAMME|" + g.prog() + "|" + g.progName() + "|4|" + g.sem());
            for (Paper p : g.papers()) {
                lines.add("PAPER|" + p.id() + "|" + p.code() + "|" + p.name() + "|" + p.credits());
            }
            for (Stu s : g.students()) {
                lines.add("STUDENT|" + s.enroll() + "|" + s.name());
                for (int i = 0; i < g.papers().length; i++) {
                    Paper p = g.papers()[i];
                    int t = s.totals()[i];
                    if (t < 0) {
                        lines.add("MARK|" + p.id() + "|0|AB|AB|");
                    } else {
                        int internal = Math.min(t, 25);          // invented split
                        int external = Math.max(0, t - internal);
                        lines.add("MARK|" + p.id() + "|" + internal + "|" + external + "|" + t + "|");
                    }
                }
            }
            lines.add("");
        }
        return lines;
    }

    /** Writes the script into a multi-page PDF so record-splitting is exercised. */
    public void generate(File out) throws IOException {
        List<String> lines = buildScript();
        try (PDDocument doc = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float margin = 50, top = 780, leading = 12, bottom = 50;
            float y = top;
            PDPage page = newPage(doc);
            PDPageContentStream cs = begin(doc, page, font);
            try {
                for (String line : lines) {
                    if (y < bottom) {
                        cs.endText();
                        cs.close();
                        page = newPage(doc);
                        cs = begin(doc, page, font);
                        y = top;
                    }
                    cs.newLineAtOffset(0, -leading);
                    cs.showText(sanitize(line));
                    y -= leading;
                }
            } finally {
                cs.endText();
                cs.close();
            }
            doc.save(out);
        }
    }

    private static PDPage newPage(PDDocument doc) {
        PDPage p = new PDPage();
        doc.addPage(p);
        return p;
    }

    private static PDPageContentStream begin(PDDocument doc, PDPage page, PDType1Font font) throws IOException {
        PDPageContentStream cs = new PDPageContentStream(doc, page);
        cs.beginText();
        cs.setFont(font, 9);
        cs.newLineAtOffset(50, 792);
        return cs;
    }

    /** Helvetica (WinAnsi) cannot encode every char; keep the fixture ASCII. */
    private static String sanitize(String s) {
        return s.replaceAll("[^\\x20-\\x7E]", " ");
    }

    /** Convenience CLI: java ... FixturePdfGenerator <out.pdf> */
    public static void main(String[] args) throws IOException {
        File out = new File(args.length > 0 ? args[0] : "sample-result.pdf");
        new FixturePdfGenerator().generate(out);
        System.out.println("Wrote " + out.getAbsolutePath());
    }
}
