package ipu.result.model;

/**
 * One student's marks in one paper. {@code grade} may be null until a
 * GradeCalculator assigns it. {@code absent} means the student did not appear.
 */
public record Mark(String enrollmentNo, String paperId, Integer internal,
                   Integer external, Integer total, String grade, boolean absent) {

    /** Copy of this mark with the grade filled in. */
    public Mark withGrade(String newGrade) {
        return new Mark(enrollmentNo, paperId, internal, external, total, newGrade, absent);
    }
}
