# GGSIPU Result Analysis & CGPA Calculator

Problem Statement 22. Analyze a GGSIPU result PDF: extract each subject's marks,
compute **SGPA** per semester and the final **CGPA**, and determine the
**subject-wise, college-wise, semester-wise and overall (university) toppers**.

**Semester-1 scope (current):** an admin — and only the admin — uploads a GGSIPU
**Internal Assessment** PDF for a course. The system reads the course and each
paper straight from the PDF and stores every student's internal marks per
subject. Upload one PDF per course; the home page then lists the courses that
have results, and anyone can look up a student's internal marksheet.

> **Under development (Semester 2).** SGPA / CGPA, grades and the topper
> analytics (overall / semester-wise / subject-wise) are switched off by default
> — they belong to the end-semester result. The engine and SQL already exist;
> flip `features.toppers=true` in `config.properties` to turn the topper pages on
> once end-semester data is available.

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

- **Semester 1 — internal assessment:** parse GGSIPU Internal Assessment PDFs
  (PDFBox + regex), the schema and JDBC DAOs, storing each student's per-subject
  internal marks, and looking them up (web result page + `console`).
- **Semester 2 — end-semester & analytics:** end-semester results, grade /
  SGPA / CGPA calculation, and the topper analyses (overall / semester-wise /
  subject-wise) — currently under development behind `features.toppers`.

Design diagrams (architecture, ERD, DFD) are in
[`design-diagrams.html`](design-diagrams.html) — open it in a browser.

## Prerequisites

- JDK **17 or newer** (the project compiles to Java 17 bytecode) — that's all
  you need.
- **Maven is not required**: the project ships with the Maven Wrapper (`mvnw`),
  which downloads the correct Maven version on first run. (If you already have
  Maven installed, you can use `mvn` instead of `./mvnw`.)
- MySQL 8 is **optional** — by default the app runs on an embedded **H2**
  database (MySQL-compatibility mode) so nothing external is needed.

## Get the code

```bash
git clone https://github.com/yugmakkar/ggsipu-result-analysis.git
```

## Build

Use the wrapper (`./mvnw` on macOS/Linux/Git Bash, `.\mvnw.cmd` on Windows
PowerShell):

```bash
./mvnw clean package     # produces target/result-analysis.jar, runs 21 tests
```

## Run

All commands go through the one jar:

```bash
# 1. make sample Internal Assessment PDFs (two courses, synthetic data)
java -jar target/result-analysis.jar gen-fixture sample-bca.pdf bca
java -jar target/result-analysis.jar gen-fixture sample-mca.pdf mca

# 2. create an admin for the web UI (password is prompted, or via ADMIN_PASSWORD)
java -jar target/result-analysis.jar create-admin admin

# 3. start the web app  ->  http://localhost:8080
java -jar target/result-analysis.jar web
#    then log in at /admin and upload the PDF(s).

# (optional) import a PDF straight from the command line instead
java -jar target/result-analysis.jar import sample-bca.pdf
```

> If you pulled an update that changed the database schema, delete the local
> `data/` folder first so it is recreated (the embedded H2 DB is for local runs).

> Run the `web` command from the project root (it serves JSPs from
> `src/main/webapp`). For a standalone deployment, place a `webapp` folder next
> to the jar.

### Web pages

- `/` — landing page: course list (data-driven) and counts.
- `/result?enroll=<no>` — a student's internal marksheet (per-subject internal
  marks); SGPA/CGPA appear here only once end-semester results are imported.
- `/toppers?type=overall|semester|subject` — topper analyses (under development,
  off by default; see `features.toppers`).
- `/login`, `/admin`, `/admin/upload` — admin login, dashboard and PDF upload
  (protected; only a logged-in admin can upload).

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

## PDF formats

Two layouts are supported and detected automatically on upload
([`docs/pdf-format.md`](docs/pdf-format.md)):

- **GGSIPU Internal Assessment sheets** (`InternalAssessmentExtractor`) — the
  real university format used for Semester 1. One PDF per course; the parser is
  the single place to re-tune if the university changes the layout.
- A synthetic **end-semester grammar** (`ResultExtractor`) feeding the dormant
  Semester-2 results/SGPA/topper path.

## Testing

```bash
mvn test                                  # unit + integration (H2 in-memory)
bash scripts/smoke-test.sh                # live HTTP + security checks (see below)
```

Smoke test (server must be running, admin created, sample PDF present):

```bash
BASE=http://localhost:8080 ADMIN_USER=admin ADMIN_PASS=... PDF=sample-bca.pdf \
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
  parser/                       PdfReader (PDFBox), InternalAssessmentExtractor,
                                ResultExtractor, fixture generators
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

## Contributing

Contributions are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md). In short:
clone, run `./mvnw clean package`, make a branch, and open a pull request (or
fork the repo if you're not a collaborator).

## License

Released under the [MIT License](LICENSE) — free to use, modify and share.
