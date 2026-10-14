# GGSIPU Result Analysis & CGPA Calculator

Problem Statement 22. Analyze a GGSIPU result PDF: extract each subject's marks,
compute **SGPA** per semester and the final **CGPA**, and determine the
**subject-wise, college-wise, semester-wise and overall (university) toppers**.

An admin uploads one result PDF containing **all courses** (many colleges,
programmes and both semesters). Anyone can then look up a student's result or
browse the topper lists.

## Tech stack

Core subjects do the work; one extra library reads the PDF.

| Subject | Used for |
|---------|----------|
| **Java** (17+) | All logic: OOP model, collections, regex, JDBC, Servlets/JSP, multithreading via the servlet container. |
| **DBMS (SQL)** | Normalised schema, PK/FK constraints, joins, aggregates, a CGPA **view**, and **window functions** (`RANK()`) for toppers. |
| **Computer Networks** | Client–server HTTP, GET/POST, multipart upload, TCP ports (8080 web, 3306 MySQL), sessions. |
| **Operating Systems + Linux** | Deployment on Ubuntu, file-system storage of PDFs, cron backup, shell scripts, the server's thread-per-request model. |
| **Discrete Structures** | Marks→grade is a *function*; tables/joins are *relations*; ranking is a *total order*; topper groups are *partitions* (by college / subject / semester). |
| Apache PDFBox | The one non-core library — reads text out of the PDF. |

## Two-semester plan

- **Semester 1 — engine + console:** PDF parsing (PDFBox + regex), the MySQL
  schema and JDBC DAOs, grade/SGPA/CGPA calculation, and a menu-driven console
  UI (`console`).
- **Semester 2 — web + analytics:** Servlets + JSP on Tomcat, admin upload, the
  four topper analyses, and deployment/security hardening.

Design diagrams (architecture, ERD, DFD) are in
[`design-diagrams.html`](design-diagrams.html) — open it in a browser.

## Prerequisites

- JDK **17 or newer** (the project compiles to Java 17 bytecode).
- Maven (to build and run tests).
- MySQL 8 is **optional** — by default the app runs on an embedded **H2**
  database (MySQL-compatibility mode) so nothing external is needed.

## Build

```bash
mvn -DskipTests package      # produces target/result-analysis.jar (runnable)
mvn test                     # run the test suite (19 tests)
```

## Run

All commands go through the one jar:

```bash
# 1. make a sample result PDF (stand-in for a real gazette)
java -jar target/result-analysis.jar gen-fixture sample-result.pdf

# 2a. Semester-1 console UI
java -jar target/result-analysis.jar console

# 2b. or import a PDF straight from the command line
java -jar target/result-analysis.jar import sample-result.pdf

# 3. create an admin for the web UI (password is prompted, or via ADMIN_PASSWORD)
java -jar target/result-analysis.jar create-admin admin

# 4. start the web app  ->  http://localhost:8080
java -jar target/result-analysis.jar web
```

> Run the `web` command from the project root (it serves JSPs from
> `src/main/webapp`). For a standalone deployment, place a `webapp` folder next
> to the jar.

### Web pages

- `/` — landing page with university-level counts.
- `/result?enroll=<no>` — a student's marksheet, SGPA per semester and CGPA.
- `/toppers?type=overall|college|semester|subject` — the four topper analyses.
- `/login`, `/admin` — admin login and the upload dashboard (protected).

## Using MySQL instead of H2

1. Create the database and a user, then load the schema:

   ```sql
   CREATE DATABASE ipu_result CHARACTER SET utf8mb4;
   CREATE USER 'ipu_app'@'localhost' IDENTIFIED BY '<password>';
   GRANT ALL ON ipu_result.* TO 'ipu_app'@'localhost';
   ```
   ```bash
   mysql -u ipu_app -p ipu_result < sql/schema.sql
   mysql -u ipu_app -p ipu_result < sql/seed.sql
   ```

2. Copy `config.example.properties` to `config.properties` and set the MySQL
   `db.url` / `db.user` / `db.password`. `config.properties` is gitignored.

The same `schema.sql` and `seed.sql` run on both MySQL and H2. (A test asserts
the runtime copies under `src/main/resources/db/` stay identical to `sql/`.)

## Grading scale & formulas

Grade bands live in the `grade_scale` **table** (see `sql/seed.sql`), so changing
the scheme means editing rows, not code.

```
credit_points(sem) = Σ (creditᵢ × grade_pointᵢ)
SGPA(sem)          = credit_points / Σ creditᵢ
CGPA               = Σ credit_points(all sems) / Σ credits(all sems)
```

Students with a backlog (an F or an absence) are excluded from the SGPA/CGPA
topper lists. **Verify the grade boundaries against your batch's ordinance.**

## Real result PDFs

`ResultExtractor` reads a clean, documented line grammar
([`docs/pdf-format.md`](docs/pdf-format.md)) that `FixturePdfGenerator` produces.
A **real** GGSIPU gazette has a messier multi-column layout, so when you have a
real PDF you re-tune only `ResultExtractor` — everything downstream is unchanged.
The current parser is **not** claimed to read real gazettes as-is.

## Testing

```bash
mvn test                                  # unit + integration (H2 in-memory)
bash scripts/smoke-test.sh                # live HTTP + security checks (see below)
```

Smoke test (server must be running, admin created, sample PDF present):

```bash
BASE=http://localhost:8080 ADMIN_USER=admin ADMIN_PASS=... PDF=sample-result.pdf \
  bash scripts/smoke-test.sh
```

## Security

Student marks are confidential, so the app includes PBKDF2 password hashing,
CSRF tokens, login lockout, upload validation (magic bytes, size, safe storage
name), output escaping, prepared statements and hardening headers. Details and
the verification checklist are in [`docs/SECURITY.md`](docs/SECURITY.md).

One deliberate trade-off: the public result lookup lets anyone query any
enrollment number. See SECURITY.md for how to restrict it.

## Project layout

```
pom.xml                         Maven build (Java 17, shaded runnable jar)
sql/                            schema.sql, seed.sql (MySQL + H2)
scripts/                        import_all.sh, backup.sh (cron), smoke-test.sh
docs/                           pdf-format.md, SECURITY.md
design-diagrams.html            architecture, ERD, DFD
src/main/java/ipu/result/
  model/                        Student, Subject, Mark, SemesterResult, College, Programme
  parser/                       PdfReader (PDFBox), ResultExtractor, FixturePdfGenerator
  service/                      GradeScale, GradeCalculator, GpaCalculator, ImportService,
                                TopperService, PasswordHasher
  dao/                          Db, StudentDAO, SubjectDAO, MarksDAO, ResultDAO, AdminDAO
  dto/                          Marksheet, Toppers
  ui/                           ConsoleApp              (Semester 1)
  web/                          Servlets, filters, WebServer  (Semester 2)
  Main.java                     single entry point / sub-commands
src/main/webapp/                JSP pages + stylesheet
src/test/java/                  JUnit tests
```

## Limitations

- The sample PDF is synthetic; the parser needs tuning for a real gazette.
- Grade boundaries must be confirmed against the official ordinance.
- Tests and local runs use H2; production targets MySQL (same SQL).
