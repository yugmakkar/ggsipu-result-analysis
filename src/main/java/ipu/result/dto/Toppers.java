package ipu.result.dto;

/**
 * Read-only rows returned by topper queries. Bean getters are included for JSP
 * EL (Tomcat 10.1 / Jakarta EL 5.0 does not read record component accessors).
 */
public final class Toppers {
    private Toppers() {}

    /** Top scorer(s) in one paper. */
    public record SubjectTopper(String paperId, String subjectName, String enrollmentNo,
                                String studentName, String collegeCode, int totalMarks, String grade) {
        public String getPaperId() { return paperId; }
        public String getSubjectName() { return subjectName; }
        public String getEnrollmentNo() { return enrollmentNo; }
        public String getStudentName() { return studentName; }
        public String getCollegeCode() { return collegeCode; }
        public int getTotalMarks() { return totalMarks; }
        public String getGrade() { return grade; }
    }

    /** Topper by SGPA or CGPA, used for college-wise, semester-wise and overall. */
    public record RankTopper(String scope, String enrollmentNo, String studentName,
                             String collegeCode, String programmeCode, double value) {
        public String getScope() { return scope; }
        public String getEnrollmentNo() { return enrollmentNo; }
        public String getStudentName() { return studentName; }
        public String getCollegeCode() { return collegeCode; }
        public String getProgrammeCode() { return programmeCode; }
        public double getValue() { return value; }
    }
}
