package ipu.result.service;

import ipu.result.model.Mark;
import ipu.result.model.SemesterResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Computes SGPA (per semester) and CGPA (across semesters).
 *
 *   credit_points = sum over papers of (credit * grade_point)
 *   SGPA          = credit_points / sum(credits)
 *   CGPA          = sum(credit_points over semesters) / sum(credits over semesters)
 */
public class GpaCalculator {

    private final GradeScale scale;

    public GpaCalculator(GradeScale scale) {
        this.scale = scale;
    }

    /**
     * Aggregates one semester. Each mark must already carry its grade.
     * {@code creditsByPaper} supplies the credit weight for every paper present.
     */
    public SemesterResult semester(String enrollmentNo, int semester,
                                   List<Mark> marks, Map<String, Integer> creditsByPaper) {
        int totalCredits = 0;
        int creditPoints = 0;
        boolean backlog = false;

        for (Mark m : marks) {
            Integer credits = creditsByPaper.get(m.paperId());
            if (credits == null) {
                throw new IllegalArgumentException("no credits known for paper " + m.paperId());
            }
            int point = m.absent() ? 0 : scale.pointFor(m.grade());
            if (m.absent() || point == 0) {
                backlog = true; // an F or an absence is a backlog
            }
            totalCredits += credits;
            creditPoints += credits * point;
        }

        double sgpa = totalCredits == 0 ? 0.0 : round2((double) creditPoints / totalCredits);
        return new SemesterResult(enrollmentNo, semester, totalCredits, creditPoints, sgpa, backlog);
    }

    /** CGPA is the credit-weighted mean across all the given semester results. */
    public static double cgpa(Collection<SemesterResult> results) {
        int credits = 0;
        int points = 0;
        for (SemesterResult r : results) {
            credits += r.totalCredits();
            points += r.creditPoints();
        }
        return credits == 0 ? 0.0 : round2((double) points / credits);
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
