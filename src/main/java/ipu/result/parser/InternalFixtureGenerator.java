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
 * Generates a synthetic GGSIPU <b>Internal Assessment</b> sheet in the real
 * portal layout (the format {@link InternalAssessmentExtractor} parses), so the
 * Semester-1 showcase can be demonstrated and tested without any real student
 * data. All names and marks are invented.
 *
 * <p>Each upload is one college + one programme; {@link #bca()} and {@link #mca()}
 * are two different courses, used to show that the system keeps several courses
 * side by side.
 */
public class InternalFixtureGenerator {

    /** One paper: 6-digit portal id, human paper code, title and internal maximum. */
    public record Paper(String paperId, String code, String name, int max) {}

    /** One course sheet. */
    public record CourseSpec(String session, int semester, int batch,
                             String collegeCode, String collegeName, String collegeShort,
                             String programmeCode, String programmeName, String programmeShort,
                             List<Paper> papers, List<String> studentNames) {}

    /** BCA, Semester 6 (two papers: a 100-mark theory and a 40-mark lab; one absence). */
    public static CourseSpec bca() {
        return new CourseSpec("JUNE, 2026", 6, 2023,
                "999", "DEMO INSTITUTE OF TECHNOLOGY, DELHI", "DIT",
                "020", "BACHELOR OF COMPUTER APPLICATIONS", "BCA",
                List.of(new Paper("020302", "BCA302", "DATA WARE HOUSING & DATA MINING", 100),
                        new Paper("098312", "BCAP312", "DATA VISUALIZATION & ANALYTICS LAB", 40)),
                List.of("AARAV SHARMA", "PRIYA", "ROHAN KUMAR VERMA", "ZARA KHAN"));
    }

    /** MCA, Semester 2 (a different college and programme). */
    public static CourseSpec mca() {
        return new CourseSpec("JUNE, 2026", 2, 2024,
                "888", "DEMO SCHOOL OF COMPUTING, DELHI", "DSC",
                "030", "MASTER OF COMPUTER APPLICATIONS", "MCA",
                List.of(new Paper("030201", "MCA201", "ADVANCED DATABASE SYSTEMS", 100),
                        new Paper("030211", "MCAP201", "ADVANCED DATABASE SYSTEMS LAB", 40)),
                List.of("ISHAAN MEHTA", "FATIMA SHEIKH", "DEV PATEL SINGH"));
    }

    /** Builds the text of one course sheet, header repeated per paper like the portal. */
    public static List<String> buildText(CourseSpec s) {
        List<String> out = new ArrayList<>();
        for (Paper p : s.papers()) {
            out.add("Guru Gobind Singh Indraprastha University, Delhi");
            out.add("Internal Assessment for Examination " + s.session());
            out.add(s.collegeName() + " (" + s.collegeCode() + ")");
            out.add(s.programmeName() + " (" + s.programmeCode() + ")");
            out.add("Paper Code : " + p.code() + "      Paper Name : " + p.name());
            out.add("BATCH SEM/ANNUAL PAPERID ENROLLMENT STUDENT NAME MAX MARKS MARKS");
            int serial = 1;
            for (String name : s.studentNames()) {
                String enroll = String.format("%03d%s%s%02d",
                        serial, s.collegeCode(), s.programmeCode(), s.batch() % 100);
                String marks = marksFor(name, p, serial);
                out.add(s.batch() + " " + s.semester() + " " + p.paperId() + " " + enroll
                        + " " + name + " " + p.max() + " " + marks);
                serial++;
            }
            out.add("Programme - " + s.programmeShort() + " (" + s.programmeCode() + ")");
            out.add("School/Institute - " + s.collegeShort() + " (" + s.collegeCode() + ")");
        }
        return out;
    }

    /** Deterministic marks; the last student is marked absent in a 40-mark lab. */
    private static String marksFor(String name, Paper p, int serial) {
        if (p.max() == 40 && serial % 4 == 0) return "A"; // one absence per lab
        int base = p.max() == 40 ? 30 : 60;
        int span = p.max() - base;
        return String.valueOf(base + (serial * 7) % (span + 1));
    }

    public void generate(File out, CourseSpec spec) throws IOException {
        List<String> lines = buildText(spec);
        try (PDDocument doc = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float top = 780, leading = 12, bottom = 50;
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
}
