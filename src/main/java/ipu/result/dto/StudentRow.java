package ipu.result.dto;

/** One row in the admin "all students" listing. */
public record StudentRow(String enrollmentNo, String name, String collegeCode,
                         String programmeCode, double cgpa, boolean hasBacklog) {
    // bean getters for JSP EL
    public String getEnrollmentNo() { return enrollmentNo; }
    public String getName() { return name; }
    public String getCollegeCode() { return collegeCode; }
    public String getProgrammeCode() { return programmeCode; }
    public double getCgpa() { return cgpa; }
    public boolean isHasBacklog() { return hasBacklog; }
}
