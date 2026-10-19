package ipu.result.parser;

import ipu.result.model.Mark;
import ipu.result.model.Subject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class InternalAssessmentParserTest {

    private ParsedResult parseBca() {
        String text = String.join("\n", InternalFixtureGenerator.buildText(InternalFixtureGenerator.bca()));
        return new InternalAssessmentExtractor().extract(text);
    }

    @Test
    void extractsCourseStudentsAndInternalMarks() {
        ParsedResult r = parseBca();

        assertEquals("JUNE, 2026", r.examSession);
        assertEquals(1, r.colleges.size());
        assertEquals(1, r.programmes.size());
        assertEquals("BCA", r.programmes.get("020").name());        // short name from the footer
        assertEquals(2, r.subjects.size());
        // students are listed once per paper row; dedup to enrollment happens at import
        assertEquals(4, r.students.stream().map(s -> s.enrollmentNo()).distinct().count());
        assertEquals(8, r.marks.size());

        Subject lab = r.subjects.get("098312");
        assertEquals("BCAP312", lab.paperCode());
        assertEquals(40, lab.internalMax());
        assertEquals(6, lab.semester());
        assertEquals("020", lab.programmeCode());

        // Multi-word name survives.
        assertTrue(r.students.stream().anyMatch(s -> s.name().equals("ROHAN KUMAR VERMA")));

        // College/programme are derived from the enrollment number itself.
        var rohan = r.students.stream().filter(s -> s.name().equals("ROHAN KUMAR VERMA")).findFirst().orElseThrow();
        assertEquals("999", rohan.collegeCode());
        assertEquals("020", rohan.programmeCode());
        assertEquals(2023, rohan.batchYear());
    }

    @Test
    void absentLabMarkIsFlagged() {
        ParsedResult r = parseBca();
        // ZARA KHAN is the 4th student -> absent in the 40-mark lab (098312).
        Mark absent = r.marks.stream()
                .filter(m -> m.paperId().equals("098312") && m.internal() == null)
                .findFirst().orElseThrow();
        assertTrue(absent.absent());
        assertNull(absent.internal());
    }

    @Test
    void roundTripThroughRealPdf(@TempDir Path tmp) throws Exception {
        File pdf = tmp.resolve("internal.pdf").toFile();
        new InternalFixtureGenerator().generate(pdf, InternalFixtureGenerator.bca());

        String text = new PdfReader().readText(pdf);
        assertTrue(text.contains(InternalAssessmentExtractor.MARKER));
        ParsedResult r = new InternalAssessmentExtractor().extract(text);

        // Structure survives PDFBox write + read.
        assertEquals(4, r.students.stream().map(s -> s.enrollmentNo()).distinct().count());
        assertEquals(2, r.subjects.size());
        assertEquals(8, r.marks.size());
    }
}
