package ipu.result.service;

import ipu.result.model.Mark;

/** Assigns a grade to a mark using a {@link GradeScale}. */
public class GradeCalculator {

    private final GradeScale scale;

    public GradeCalculator(GradeScale scale) {
        this.scale = scale;
    }

    /**
     * Returns the mark with its grade filled in.
     * An absent paper is always graded F. If the PDF already carried a grade it
     * is kept; otherwise the grade is derived from the total marks.
     */
    public Mark assign(Mark mark) {
        if (mark.absent()) {
            return mark.withGrade("F");
        }
        if (mark.grade() != null && !mark.grade().isBlank()) {
            return mark; // trust the grade printed in the result
        }
        if (mark.total() == null) {
            throw new IllegalArgumentException(
                    "cannot grade " + mark.paperId() + " for " + mark.enrollmentNo()
                    + ": no total marks and not marked absent");
        }
        return mark.withGrade(scale.gradeFor(mark.total()));
    }
}
