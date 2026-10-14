package ipu.result.model;

/** Aggregated result for one student in one semester. */
public record SemesterResult(String enrollmentNo, int semester, int totalCredits,
                             int creditPoints, double sgpa, boolean hasBacklog) {}
