package ipu.result.parser;

import ipu.result.model.Mark;
import ipu.result.model.Subject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser tests for the end-semester Result Tabulation Sheet. All data here is
 * synthetic: institution code 999, programme 999, invented names and SIDs. It
 * reproduces the exact line shapes PDFBox yields from a real gazette (the three
 * flattened lines per student) so the alignment logic is exercised without any
 * real student data.
 */
class GazetteResultExtractorTest {

    /** A one-college, one-semester gazette with a normal, an absent and a grace-marks student. */
    private static final String GAZETTE = String.join("\n",
            "RESULT TABULATION SHEET",
            "LEGEND",
            "A: Absent     C: Cancelled",
            " (SCHEME OF EXAMINATIONS)",
            "Scheme of Programme Code: 999     Programme Name: DEMO PROGRAMME      SchemeID: 199920240001     Sem./Year: 01 SEMESTER",
            "Institution Code: 999     Institution: DEMO INSTITUTE OF TECHNOLOGY",
            "S.No. Paper ID Code Subject Credit Type Exam Mode Kind Minor Major Max. Marks Pass Marks",
            "01 999101 DMO-101 DEMO THEORY ONE  4 THEORY UES COMPULSORY MANDATORY  25  75  100  40 ",
            "02 999103 DMO-103 DEMO THEORY TWO  4 THEORY UES COMPULSORY MANDATORY  25  75  100  40 ",
            "03 999161 DMO-161 DEMO LAB  2 PRACTICAL UES COMPULSORY MANDATORY  40  60  100  40 ",
            "04 999171 DMO-171 DEMO PROFICIENCY  1 PRACTICAL NUES COMPULSORY MANDATORY --  100  100  40 ",
            "RESULT TABULATION SHEET",
            "Result of Programme Code: 999     Programme Name: DEMO PROGRAMME     Sem./Year: 01 SEMESTER     Batch: 2024     Examination: REGULAR March, 2025",
            "S.No. Photo. Roll no./Name Institution Code: 999     Institution: DEMO INSTITUTE OF TECHNOLOGY CS/Remarks",
            // normal student: all passed
            "00199999924 999101(4) 999103(4) 999161(2) 999171(1)  11 ",
            "AARAV DEMO",
            "SID: 000000000001  20  53  22  60  35  50 -  84 ",
            "SchemeID: 199920240001",
            " 1 73(A) 82(A+) 85(A+) 84(A+)",
            // absent student: absent in the three exam papers, passed only the NUES paper
            "00299999924 999101(4) 999103(4) 999161(2) 999171(1)  1 ",
            "BHAVYA DEMO NAME",                 // multi-word name
            "SID: 000000000002  17 A  16 A  31 A -  77 ",
            "SchemeID: 199920240001",
            " 2 A A A 77(A+)",
            // grace-marks student: paper one is 40(P)* (grace lifts 38 -> 40)
            "00399999924 999101(4) 999103(4) 999161(2) 999171(1)  11 ",
            "CHARU DEMO",
            "SID: 000000000003  10  28  22  60  35  50 -  84 ",
            "SchemeID: 199920240001",
            " 3 40(P)* 82(A+) 85(A+) 84(A+)");

    @Test
    void parsesSchemeStudentsAndMarksWithoutMisalignment() {
        GazetteResultExtractor ex = new GazetteResultExtractor();
        ParsedResult r = ex.extract(GAZETTE);

        assertEquals("REGULAR March, 2025", r.examSession);
        assertEquals(1, r.colleges.size());
        assertEquals("DEMO INSTITUTE OF TECHNOLOGY", r.colleges.get("999").name());
        assertEquals(4, r.subjects.size());
        assertEquals(3, r.students.size());
        assertEquals(12, r.marks.size());

        // The alignment invariants the parser guards must all hold.
        assertEquals(3, ex.stats().students);
        assertEquals(0, ex.stats().tokenMismatches);
        assertEquals(0, ex.stats().sumMismatches);

        // Credits and the internal maximum come from the scheme table.
        Subject theory = r.subjects.get("999101");
        assertEquals("DMO-101", theory.paperCode());
        assertEquals("DEMO THEORY ONE", theory.name());
        assertEquals(4, theory.credits());
        assertEquals(25, theory.internalMax());
        assertEquals(1, theory.semester());
        assertNull(r.subjects.get("999171").internalMax());   // NUES paper: "--" -> no internal

        // College and programme derive from the enrollment number itself.
        var aarav = r.students.stream().filter(s -> s.name().equals("AARAV DEMO")).findFirst().orElseThrow();
        assertEquals("999", aarav.collegeCode());
        assertEquals("999", aarav.programmeCode());
        assertTrue(r.students.stream().anyMatch(s -> s.name().equals("BHAVYA DEMO NAME")));
    }

    @Test
    void splitsInternalExternalAndTrustsPrintedGrade() {
        ParsedResult r = new GazetteResultExtractor().extract(GAZETTE);
        Mark p1 = mark(r, "00199999924", "999101");
        assertEquals(20, p1.internal());
        assertEquals(53, p1.external());
        assertEquals(73, p1.total());
        assertEquals("A", p1.grade());          // grade "A" (65-74), not the absent marker
        assertFalse(p1.absent());

        Mark nues = mark(r, "00199999924", "999171");
        assertNull(nues.internal());            // "-" internal placeholder
        assertEquals(84, nues.external());
        assertEquals(84, nues.total());
    }

    @Test
    void flagsAbsentPaperFromBareMarker() {
        ParsedResult r = new GazetteResultExtractor().extract(GAZETTE);
        Mark p1 = mark(r, "00299999924", "999101");
        assertTrue(p1.absent());
        assertNull(p1.total());
        assertEquals(17, p1.internal());        // internal was recorded, exam was missed
        // the NUES paper was still passed
        assertFalse(mark(r, "00299999924", "999171").absent());
    }

    @Test
    void keepsGraceTotalWithoutSumMismatch() {
        GazetteResultExtractor ex = new GazetteResultExtractor();
        ParsedResult r = ex.extract(GAZETTE);
        Mark p1 = mark(r, "00399999924", "999101");
        assertEquals(40, p1.total());           // grace-lifted total is kept verbatim
        assertEquals("P", p1.grade());
        assertEquals(10, p1.internal());
        assertEquals(28, p1.external());         // 10+28 = 38, below 40: grace, not a parse error
        assertEquals(0, ex.stats().sumMismatches);
    }

    private static Mark mark(ParsedResult r, String enroll, String paperId) {
        return r.marks.stream()
                .filter(m -> m.enrollmentNo().equals(enroll) && m.paperId().equals(paperId))
                .findFirst().orElseThrow();
    }
}
