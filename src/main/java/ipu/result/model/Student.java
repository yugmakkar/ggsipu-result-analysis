package ipu.result.model;

/** A student, identified by enrollment number. */
public record Student(String enrollmentNo, String name, String collegeCode,
                      String programmeCode, Integer batchYear) {}
