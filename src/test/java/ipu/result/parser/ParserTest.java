package ipu.result.parser;

import ipu.result.model.Mark;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    @Test
    void extractFromScriptText() {
        String text = String.join("\n", FixturePdfGenerator.buildScript());
        ParsedResult r = new ResultExtractor().extract(text);

        assertEquals("December 2024", r.examSession);
        assertEquals(2, r.colleges.size());          // 101, 164
        assertEquals(2, r.programmes.size());         // 020, 021
        assertEquals(12, r.subjects.size());          // 3 papers * 2 programmes * 2 semesters
        assertEquals(16, r.students.size());          // 8 students * 2 semester blocks
        assertEquals(48, r.marks.size());             // 16 * 3 papers

        // Vivaan Gupta was absent in paper 99103
        Mark absent = r.marks.stream()
                .filter(m -> m.enrollmentNo().equals("01201012024") && m.paperId().equals("99103"))
                .findFirst().orElseThrow();
        assertTrue(absent.absent());
        assertNull(absent.total());
    }

    @Test
    void roundTripThroughRealPdf(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        File pdf = tmp.resolve("sample.pdf").toFile();
        new FixturePdfGenerator().generate(pdf);
        assertTrue(pdf.length() > 0);

        // magic bytes are a PDF
        byte[] head = new byte[5];
        try (var in = Files.newInputStream(pdf.toPath())) { in.read(head); }
        assertEquals("%PDF-", new String(head));

        String text = new PdfReader().readText(pdf);
        ParsedResult r = new ResultExtractor().extract(text);

        // Same structure survives PDFBox write + read (and page splitting).
        assertEquals(2, r.colleges.size());
        assertEquals(12, r.subjects.size());
        assertEquals(48, r.marks.size());
    }
}
