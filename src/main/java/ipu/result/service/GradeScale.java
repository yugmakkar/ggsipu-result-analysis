package ipu.result.service;

import java.util.ArrayList;
import java.util.List;

/**
 * The marks-to-grade mapping. In Discrete Structures terms this is a function
 * from a mark (0-100) to a grade, and from a grade to a grade point.
 * The bands are data, loaded from the grade_scale table (or the default below),
 * so changing the scheme never means changing code.
 */
public class GradeScale {

    /** One grade band: marks in [min, max] map to this grade and point. */
    public record Band(String grade, int min, int max, int point) {}

    private final List<Band> bands;

    public GradeScale(List<Band> bands) {
        if (bands == null || bands.isEmpty()) {
            throw new IllegalArgumentException("grade scale must have at least one band");
        }
        this.bands = new ArrayList<>(bands);
    }

    /** Grade for a total mark. Throws if no band covers it. */
    public String gradeFor(int marks) {
        for (Band b : bands) {
            if (marks >= b.min() && marks <= b.max()) {
                return b.grade();
            }
        }
        throw new IllegalArgumentException("no grade band covers marks=" + marks);
    }

    /** Grade point for a grade letter. Throws if the grade is unknown. */
    public int pointFor(String grade) {
        for (Band b : bands) {
            if (b.grade().equalsIgnoreCase(grade)) {
                return b.point();
            }
        }
        throw new IllegalArgumentException("unknown grade: " + grade);
    }

    public List<Band> bands() {
        return List.copyOf(bands);
    }

    /** Matches sql/seed.sql. Used for tests and as a fallback when the DB is empty. */
    public static GradeScale defaultScale() {
        return new GradeScale(List.of(
                new Band("O",  90, 100, 10),
                new Band("A+", 75,  89,  9),
                new Band("A",  65,  74,  8),
                new Band("B+", 55,  64,  7),
                new Band("B",  50,  54,  6),
                new Band("C",  45,  49,  5),
                new Band("P",  40,  44,  4),
                new Band("F",   0,  39,  0)
        ));
    }
}
