-- GGSIPU Result Analysis - database schema
-- Written to run on BOTH MySQL 8 and H2 (MODE=MySQL). Standard SQL only.
-- Run order: this file, then seed.sql.

CREATE TABLE IF NOT EXISTS college (
    college_code  VARCHAR(10)  PRIMARY KEY,
    college_name  VARCHAR(150) NOT NULL
);

CREATE TABLE IF NOT EXISTS programme (
    programme_code  VARCHAR(10)  PRIMARY KEY,
    programme_name  VARCHAR(150) NOT NULL,
    duration_years  INT          NOT NULL DEFAULT 4
);

CREATE TABLE IF NOT EXISTS student (
    enrollment_no   VARCHAR(20)  PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    college_code    VARCHAR(10)  NOT NULL,
    programme_code  VARCHAR(10)  NOT NULL,
    batch_year      INT,
    CONSTRAINT fk_student_college   FOREIGN KEY (college_code)   REFERENCES college(college_code),
    CONSTRAINT fk_student_programme FOREIGN KEY (programme_code) REFERENCES programme(programme_code)
);

-- Grade boundaries are DATA, not code: change the scheme by editing rows.
CREATE TABLE IF NOT EXISTS grade_scale (
    grade        VARCHAR(2) PRIMARY KEY,
    min_marks    INT NOT NULL,
    max_marks    INT NOT NULL,
    grade_point  INT NOT NULL
);

CREATE TABLE IF NOT EXISTS subject (
    paper_id        VARCHAR(15)  PRIMARY KEY,
    paper_code      VARCHAR(20),
    subject_name    VARCHAR(150) NOT NULL,
    credits         INT          NOT NULL,
    semester        INT          NOT NULL,
    programme_code  VARCHAR(10)  NOT NULL,
    CONSTRAINT fk_subject_programme FOREIGN KEY (programme_code) REFERENCES programme(programme_code)
);

-- One upload = one imported PDF.
CREATE TABLE IF NOT EXISTS result_upload (
    upload_id     INT AUTO_INCREMENT PRIMARY KEY,
    file_name     VARCHAR(255) NOT NULL,
    stored_name   VARCHAR(255) NOT NULL,
    exam_session  VARCHAR(50),
    uploaded_on   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    row_count     INT          NOT NULL DEFAULT 0
);

-- Resolves the many-to-many between student and subject.
CREATE TABLE IF NOT EXISTS marks (
    enrollment_no   VARCHAR(20) NOT NULL,
    paper_id        VARCHAR(15) NOT NULL,
    upload_id       INT,
    internal_marks  INT,
    external_marks  INT,
    total_marks     INT,
    grade           VARCHAR(2),
    absent          BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (enrollment_no, paper_id),
    CONSTRAINT fk_marks_student FOREIGN KEY (enrollment_no) REFERENCES student(enrollment_no),
    CONSTRAINT fk_marks_subject FOREIGN KEY (paper_id)      REFERENCES subject(paper_id),
    CONSTRAINT fk_marks_upload  FOREIGN KEY (upload_id)     REFERENCES result_upload(upload_id),
    CONSTRAINT fk_marks_grade   FOREIGN KEY (grade)         REFERENCES grade_scale(grade)
);

-- Per-student, per-semester SGPA. CGPA is derived (see view below).
CREATE TABLE IF NOT EXISTS semester_result (
    enrollment_no  VARCHAR(20) NOT NULL,
    semester       INT         NOT NULL,
    total_credits  INT         NOT NULL,
    credit_points  INT         NOT NULL,
    sgpa           DECIMAL(4,2) NOT NULL,
    has_backlog    BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (enrollment_no, semester),
    CONSTRAINT fk_semresult_student FOREIGN KEY (enrollment_no) REFERENCES student(enrollment_no)
);

-- Admin accounts for the web layer. Passwords are PBKDF2 hashes, never plaintext.
CREATE TABLE IF NOT EXISTS admin (
    admin_id       INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    created_on     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failed_logins  INT NOT NULL DEFAULT 0,
    locked_until   TIMESTAMP NULL
);

-- CGPA: credit-weighted mean of grade points across all semesters. Derived, not stored.
CREATE OR REPLACE VIEW v_cgpa AS
SELECT enrollment_no,
       SUM(total_credits)                                   AS total_credits,
       SUM(credit_points)                                   AS total_points,
       ROUND(SUM(credit_points) * 1.0 / SUM(total_credits), 2) AS cgpa,
       MAX(CASE WHEN has_backlog THEN 1 ELSE 0 END)          AS has_backlog
FROM semester_result
GROUP BY enrollment_no;
