package ipu.result.service;

import ipu.result.model.Mark;
import ipu.result.model.SemesterResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GradeAndGpaTest {

    private final GradeScale scale = GradeScale.defaultScale();
    private final GradeCalculator grader = new GradeCalculator(scale);
    private final GpaCalculator gpa = new GpaCalculator(scale);

    private Mark raw(String paper, Integer total) {
        return new Mark("ENR1", paper, null, null, total, null, false);
    }

    @Test
    void gradeBoundaries() {
        assertEquals("O",  scale.gradeFor(90));
        assertEquals("O",  scale.gradeFor(100));
        assertEquals("A+", scale.gradeFor(89));
        assertEquals("A+", scale.gradeFor(75));
        assertEquals("P",  scale.gradeFor(40));
        assertEquals("F",  scale.gradeFor(39));
        assertEquals("F",  scale.gradeFor(0));
    }

    @Test
    void gradeDerivedWhenMissing() {
        assertEquals("A+", grader.assign(raw("P1", 80)).grade());
    }

    @Test
    void printedGradeIsKept() {
        Mark m = new Mark("ENR1", "P1", 20, 60, 80, "O", false); // PDF said O even though 80
        assertEquals("O", grader.assign(m).grade());
    }

    @Test
    void absentIsGradedFAndCausesBacklog() {
        Mark absent = new Mark("ENR1", "P1", null, null, null, null, true);
        Mark graded = grader.assign(absent);
        assertEquals("F", graded.grade());

        SemesterResult r = gpa.semester("ENR1", 1, List.of(graded), Map.of("P1", 4));
        assertTrue(r.hasBacklog());
        assertEquals(0.0, r.sgpa());
    }

    @Test
    void sgpaIsCreditWeighted() {
        // P1: 4 credits, 85 -> A+ (9);  P2: 2 credits, 60 -> B+ (7)
        Mark p1 = grader.assign(raw("P1", 85));
        Mark p2 = grader.assign(raw("P2", 60));
        SemesterResult r = gpa.semester("ENR1", 1, List.of(p1, p2), Map.of("P1", 4, "P2", 2));

        // points = 4*9 + 2*7 = 50 ; credits = 6 ; sgpa = 8.333 -> 8.33
        assertEquals(6, r.totalCredits());
        assertEquals(50, r.creditPoints());
        assertEquals(8.33, r.sgpa());
        assertFalse(r.hasBacklog());
    }

    @Test
    void failingPaperFlagsBacklog() {
        Mark pass = grader.assign(raw("P1", 85));
        Mark fail = grader.assign(raw("P2", 30)); // F
        SemesterResult r = gpa.semester("ENR1", 1, List.of(pass, fail), Map.of("P1", 4, "P2", 4));
        assertTrue(r.hasBacklog());
    }

    @Test
    void cgpaAcrossSemesters() {
        // Sem1: credits 20, points 180 ; Sem2: credits 20, points 160
        SemesterResult s1 = new SemesterResult("ENR1", 1, 20, 180, 9.0, false);
        SemesterResult s2 = new SemesterResult("ENR1", 2, 20, 160, 8.0, false);
        // cgpa = 340 / 40 = 8.5
        assertEquals(8.5, GpaCalculator.cgpa(List.of(s1, s2)));
    }

    @Test
    void cgpaIsCreditWeightedNotAverageOfSgpa() {
        // Unequal credits: average of SGPAs would mislead.
        SemesterResult s1 = new SemesterResult("ENR1", 1, 10, 100, 10.0, false); // 10 credits @10
        SemesterResult s2 = new SemesterResult("ENR1", 2, 30, 120, 4.0, false);  // 30 credits @4
        // weighted = 220/40 = 5.5  (simple average would be 7.0)
        assertEquals(5.5, GpaCalculator.cgpa(List.of(s1, s2)));
    }
}
