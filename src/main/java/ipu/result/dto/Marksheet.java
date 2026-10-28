package ipu.result.dto;

import java.util.List;

/**
 * Everything shown on a student's result page.
 *
 * JavaBean-style getters are provided alongside the record accessors because
 * Jakarta EL 5.0 (Tomcat 10.1) resolves JSP expressions via getters, not record
 * component accessors.
 */
public record Marksheet(String enrollmentNo, String name, String collegeCode, String collegeName,
                        String programmeCode, String programmeName, double cgpa, boolean hasBacklog,
                        boolean internalOnly, List<SemesterBlock> semesters) {

    public String getEnrollmentNo() { return enrollmentNo; }
    public String getName() { return name; }
    public String getCollegeCode() { return collegeCode; }
    public String getCollegeName() { return collegeName; }
    public String getProgrammeCode() { return programmeCode; }
    public String getProgrammeName() { return programmeName; }
    public double getCgpa() { return cgpa; }
    public boolean isHasBacklog() { return hasBacklog; }
    /** True when only internal-assessment marks exist (no end-semester results yet). */
    public boolean isInternalOnly() { return internalOnly; }
    public List<SemesterBlock> getSemesters() { return semesters; }

    public record SemesterBlock(int semester, double sgpa, boolean hasBacklog, List<MarkRow> rows) {
        public int getSemester() { return semester; }
        public double getSgpa() { return sgpa; }
        public boolean isHasBacklog() { return hasBacklog; }
        public List<MarkRow> getRows() { return rows; }

        /**
         * True when this semester's end-semester result is "declared" and the page
         * should show SGPA; false shows "result awaited" instead (and avoids a
         * misleading "SGPA 0.00"). A row counts as having an end-semester result
         * when it carries a total, a grade, or an absence.
         */
        public boolean isDeclared() {
            for (MarkRow r : rows) {
                if (r.absent() || r.total() != null || (r.grade() != null && !r.grade().isBlank())) {
                    return true;
                }
            }
            return false;
        }
    }

    public record MarkRow(String paperId, String paperCode, String subjectName, int credits, Integer internalMax,
                          Integer internal, Integer external, Integer total, String grade, boolean absent) {
        public String getPaperId() { return paperId; }
        public String getPaperCode() { return paperCode; }
        public String getSubjectName() { return subjectName; }
        public int getCredits() { return credits; }
        public Integer getInternalMax() { return internalMax; }
        public Integer getInternal() { return internal; }
        public Integer getExternal() { return external; }
        public Integer getTotal() { return total; }
        public String getGrade() { return grade; }
        public boolean isAbsent() { return absent; }
    }
}
