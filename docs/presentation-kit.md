# Presentation Kit — GGSIPU Result Analysis & CGPA Calculator

Internal brief for the 3-person team review. Two things this file is for:

1. **Who owns what** (40 / 30 / 10 split + a declared AI slice).
2. **The Q&A bank** — every answer below points at something that actually exists
   in the repo, so nobody has to improvise.

---

## 0. The one rule that makes all of this safe

Do not claim code you cannot explain, and **declare the AI share in the deck**.
The 20% AI slice is the strongest slide you have: "AI produced first drafts of the
boilerplate — JSPs, CSS, fixture generators, docs, test skeletons — and we
designed, reviewed and debugged everything." A team that says that is
unassailable. A team that hides it and then fumbles one question loses the whole
review. Every answer in this kit is verifiable by opening the file, so
"we can show you" is always available.

---

## 1. Role split (40 / 30 / 10 + 20 AI = 100)

| # | Person | Share | Owns | Files they must be able to open & explain |
|---|--------|-------|------|-------------------------------------------|
| A | **You** | **40%** | Core pipeline & robustness: PDF extraction, import transaction, integration | `parser/InternalAssessmentExtractor.java`, `parser/GazetteResultExtractor.java`, `parser/PdfReader.java`, `service/ImportService.java`, `parser/ParsedResult.java` |
| B | **Member 2** | **30%** | Data model & analytics: schema, normalization, grading, SGPA/CGPA, ranking | `sql/schema.sql`, `service/GradeScale.java`, `service/GradeCalculator.java`, `service/GpaCalculator.java`, `dao/ResultDAO.java` |
| C | **Member 3** | **10%** | Web layer, security wiring, build & demo operations | `web/WebServer.java`, `web/AuthFilter.java`, `web/UploadServlet.java`, `web/Csrf.java`, `service/PasswordHasher.java`, `scripts/*.sh` |
| — | **AI** | **20%** | First drafts of boilerplate: JSP + CSS, fixture generators, README/docs, test skeletons | `src/main/webapp/**`, `parser/InternalFixtureGenerator.java`, `docs/**`, `README.md` |

Why this split is defensible rather than arbitrary: A's work is the part that can
fail silently and wrongly (parsing), B's is the part that can be logically wrong
(math/ranking), C's is the part that can be insecure — and C is also the person
driving the live demo, which is real work on the day.

**Speaking order = A, B, C.** You open (problem + architecture + the parser story,
the highest-value 3 minutes), B carries the "correctness" section, C closes with
security + demo so the last thing they see is a working product.

---

## 2. Slide plan (~11 slides, 10–12 minutes)

| # | Slide | Speaker | ~min |
|---|-------|---------|------|
| 1 | Title + one-line problem statement (PS 22) | A | 0:20 |
| 2 | Problem: a PDF is not data — what an admin actually does today | A | 0:50 |
| 3 | Architecture: one jar, 4 layers (parser → service → dao → web) | A | 1:00 |
| 4 | Hardest problem: parsing a PDF that has no table structure | A | 1:40 |
| 5 | Data model + ERD (7 tables, 1 view), why normalized | B | 1:20 |
| 6 | Grading & GPA: the two formulas, grade scale as *data* | B | 1:00 |
| 7 | Toppers: `RANK()` window functions, ties, backlogs | B | 1:20 |
| 8 | Security: 8 threats, 8 mitigations (the table) | C | 1:20 |
| 9 | What AI did / what we did | C | 1:00 |
| 10 | Live demo or screenshots + numbers | C | 1:30 |
| 11 | Limitations & what Semester 2 adds | A | 0:50 |

Keep slides visual: ERD from `design-diagrams.html`, one screenshot of the
marksheet, one screenshot of the upload dashboard, and the security table. Put
code on screen only for slide 4 (the 3-line gazette block annotation).

---

## 3. Talk tracks

### A — opening, architecture, the parser (40%)

> "The problem statement asks us to analyse a GGSIPU result PDF. The real
> difficulty is not the CGPA maths — it's that **a PDF has no rows and columns**.
> It has glyphs at coordinates. We get one long stream of text lines and have to
> reconstruct the table.
>
> Our architecture is four layers behind a single jar: `parser` turns PDF text
> into a `ParsedResult` object, `service` grades and computes, `dao` persists with
> prepared statements, `web` is Servlets/JSP on embedded Tomcat. The parser never
> touches the database, so we can unit-test it on raw strings."

Then slide 4 — the money slide:

> "The end-semester gazette is the hard one. One student's visual row is flattened
> by PDFBox into **three** lines: an anchor line with the 11-digit enrollment and
> each `paper(credits)` token, a `SID:` line with internal and external marks
> interleaved two-per-paper, and a totals line with one `total(grade)` token per
> paper.
>
> We don't trust the page headers. The college and programme codes are **derived
> from the enrollment number itself** — positions 4–6 and 7–9. So even if a header
> is misread or repeated across pages, every student is still filed correctly.
>
> And we made it **fail closed**: before writing anything we check that the token
> count equals 2 × papers on the SID line and exactly one totals token per paper.
> If a block is short, we throw a `ParseException` — the whole import rolls back
> and the uploaded file is deleted. We would rather reject a sheet than silently
> attach the wrong marks to a student."

### B — data model, grading, ranking (30%)

> "Seven tables and one view. `marks` is the many-to-many between `student` and
> `subject`, keyed on `(enrollment_no, paper_id)`. `semester_result` holds SGPA per
> student per semester, and **CGPA is a view, not a column** — it's derived by
> summing credits and credit-points, so it can never drift out of sync with the
> rows it comes from.
>
> Grading is a function from marks to a grade, and the bands live in the
> `grade_scale` **table**, not in code. If the ordinance changes, we edit rows —
> no redeploy.
>
> SGPA = Σ(credit × grade_point) / Σ(credits), rounded HALF_UP to 2 decimals with
> `BigDecimal` — never `double` arithmetic for a published number.
>
> For toppers we use `RANK() OVER (PARTITION BY ...)` — deliberately `RANK` and not
> `ROW_NUMBER`, so two students on identical marks **both** appear as topper
> instead of one being arbitrarily dropped. Four queries: overall, per-semester,
> per-college, per-subject, each partitioned accordingly and each filtering out
> `has_backlog`."

### C — security, AI share, demo (10%)

> "Student marks are confidential, so we treated the web layer as a threat model,
> not a form. Eight mitigations: prepared statements everywhere; PBKDF2-HMAC-SHA256
> at 120k iterations with a per-user salt, verified in constant time;
> `changeSessionId()` on login to kill session fixation; lockout after 5 failed
> logins; a per-session CSRF token compared in constant time and required on every
> POST; upload validation by `%PDF-` magic bytes plus a **server-generated UUID**
> filename so a crafted name can't traverse paths; JSTL `<c:out>` on every dynamic
> value; and hardening headers via a filter.
>
> We also ran `scripts/smoke-test.sh` against the live server to prove each of
> those actually behaves — that's our evidence, not just our intent."

### The AI slide (C, 1 minute) — say it plainly

> "On tooling: roughly a fifth of the line count started as AI-generated drafts —
> the JSP markup, the stylesheet, the synthetic PDF fixture generators, and the
> first pass of the docs and test skeletons. What AI did **not** do is the design:
> the parser strategy, the schema and the ranking SQL are ours. And every AI
> output was reviewed, and most of it was rewritten after it failed — for example
> the schema re-seed bug where restarting the server after an import would try to
> `DELETE FROM grade_scale` and die on the foreign key. That's why there's now a
> `SchemaInitIdempotentTest`."

That last sentence is a gift: it turns "AI wrote it" into "we found and fixed a
real bug", which is exactly what a reviewer wants to hear.

---

## 4. Q&A bank — answers with receipts

**Q: Why regex instead of a proper PDF table library?**
Because there is no table in the PDF — only positioned glyphs. PDFBox gives us
text with `setSortByPosition(true)`; a table extractor would still need to be
taught this gazette's column model. The regexes are anchored on things the
university cannot vary quietly (11-digit enrollment, 6-digit paper id, the
`SID:` prefix, `total(grade)` shape), and everything is centralized in one class
so re-tuning is a one-file change. Receipts: `GazetteResultExtractor.java`.

**Q: What happens if the PDF is malformed or a different format?**
Three parsers, chosen by marker text in `ImportService.importPdf`: internal
assessment (`"Internal Assessment for Examination"`), gazette
(`"RESULT TABULATION SHEET"`), else the legacy pipe grammar. If nothing parses,
we throw instead of committing an empty upload. Parsing completes **before** any
DB write, so a parse error means zero database work.

**Q: Someone re-uploads a corrected gazette. Do you duplicate rows?**
No. `upsertEndSem` updates on `(enrollment_no, paper_id)`, leaving the student's
other papers intact — important because a revised sheet often re-states only one
or two papers. Then SGPA is recomputed from **all** declared marks for that
semester in the DB (`gradedMarksForSemester`), not just the ones in this PDF.

**Q: Two students have identical marks. Who's the topper?**
Both — `RANK()`, not `ROW_NUMBER()`. Ties share rank 1 and both rows are returned.

**Q: How is a backlog handled?**
An absence or an F sets `has_backlog` on the semester result and gives grade
point 0. Topper queries filter `has_backlog = 0`, so a backlog student is excluded
from ranking but still appears on their own marksheet.

**Q: Why is CGPA not a stored column?**
It's derived — a stored copy can go stale on the next import. `v_cgpa` sums
credits and credit-points across all semesters, so it's always consistent with
`semester_result`. Storage would be a caching decision, and we didn't need it.

**Q: What stops SQL injection?**
Every query is a `PreparedStatement` with bound parameters. The only string-built
SQL is `SELECT COUNT(*) FROM <table>` where the table name is one of five
internal constants, never user input — it's commented as such in `ResultDAO`.

**Q: What stops a malicious upload?**
Size cap from config, `%PDF-` magic-byte check, and a UUID storage filename
generated server-side so the submitted name is display-only — no path traversal.
Files live outside the webapp directory so they aren't served as static content.
On import failure the stored file is deleted and the transaction is rolled back.

**Q: What's the biggest security weakness?**
Two, and both are documented in `docs/SECURITY.md`. First, `/result` lets anyone
query any enrollment number — enrollment numbers are sequential, so the range can
be walked. We chose that deliberately to mirror how GGSIPU publishes results, and
the document states the fix: put `/result` behind auth or require a second factor.
Second, the demo admin `admin`/`admin12345` is public; it's disabled with
`DEMO_ADMIN=false` and it never overwrites an existing admin.
*(Answering this well is worth more than any slide — it shows you did a threat
model instead of ticking boxes.)*

**Q: How do you know it still works?**
38 `@Test` methods across 7 test classes: parser fixtures, grade/GPA maths, import
+ topper integration on in-memory H2, schema-init idempotency, and
`SchemaParityTest`, which asserts the runtime copies of `schema.sql`/`seed.sql`
stay byte-identical to `sql/` so the two can't drift. Plus `smoke-test.sh` for
live HTTP and security behaviour.
⚠️ `README.md` says "runs 21 tests" — that's stale. Say 38, or fix the line.

**Q: Why embedded H2 and embedded Tomcat?**
Zero-setup reproducibility: `java -jar result-analysis.jar web` works on a clean
machine with only a JDK. Production just points `db.url` at MySQL — same SQL,
same schema file. That's also why `SchemaParityTest` exists.

**Q: Have you tested it on a real university gazette?**
Be honest: the shipped samples are synthetic; the gazette parser is written
against the real *Result Tabulation Sheet* layout and its consistency checks are
built for real-world messiness (electives, absences, grace marks marked with `*`),
but the honest statement is "needs validation on more real sheets" — which is
listed under Limitations. Do not claim broad real-world coverage.

**Q: How would this scale to the whole university?**
Marks are written row-by-row today; the first change is JDBC batching
(`addBatch`/`executeBatch`). The schema already supports multiple colleges and
programmes, and the topper queries are single-pass window functions, so the
database side is fine. Say this as a known limitation, not a surprise.

---

## 5. Each person's own five — rehearse out loud

**A (you):** the 3-line gazette block · why enrollment positions 4–6 and 7–9 ·
what `flush()` validates · why extracting the file *before* the DB write matters ·
what `ParsedResult` contains and why the parser has no DB dependency.

**B:** why `marks` is keyed on both columns · what `v_cgpa` computes · SGPA vs
CGPA formula · `RANK` vs `ROW_NUMBER` · why the grade scale is a table.

**C:** what CSRF protects and where the token is checked · PBKDF2 params and why
constant-time compare · why the stored filename is generated, not submitted ·
what `changeSessionId()` prevents · which headers and why.

## 6. Do not say

- ❌ "The AI built it." / ❌ "We built 100% of it by hand." → ✅ "AI drafted the
  boilerplate; we designed, reviewed and debugged."
- ❌ "It parses any GGSIPU PDF." → ✅ "Two real formats, detected by marker, with
  fail-closed validation; more real sheets still need testing."
- ❌ "CGPA is live on the site." → ✅ "The engine and SQL are done; end-semester
  data is gated behind `features.toppers` for Semester 2."
- ❌ "We used machine learning." → ✅ deterministic regex + state machine, which
  is the *right* choice here because it's debuggable and auditable.
- ❌ Inventing a test count or a metric you can't reproduce on screen.

## 7. Pre-flight, the night before

1. `./mvnw clean package` → succeeds, `target/result-analysis.jar` exists.
2. `java -jar target/result-analysis.jar gen-fixture sample-bca.pdf bca` then
   `import sample-bca.pdf` — confirm the counts it prints.
3. `java -jar target/result-analysis.jar web` → log in, upload, open a marksheet.
   **Do the demo twice**; the first run always surprises someone.
4. Delete `data/` if the schema changed since your last run.
5. Fix the stale "21 tests" line in `README.md` if the README will be on screen.
6. Know your own numbers: 7 tables + 1 view, 4 layers, 3 parsers, 38 tests,
   8 security mitigations.
