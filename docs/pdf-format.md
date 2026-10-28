# Result PDF formats

The app recognises three PDF layouts. The format is detected automatically on
upload (from the text PDFBox extracts) and routed to the matching parser.

1. **GGSIPU Internal Assessment sheet** (`InternalAssessmentExtractor`, marker
   `Internal Assessment for Examination`) — the real mid-semester internal-marks
   format. See *Internal Assessment format* below.
2. **GGSIPU Result Tabulation Sheet** (`GazetteResultExtractor`, marker
   `RESULT TABULATION SHEET`) — the real end-semester result gazette the
   university publishes once a result is declared. This is what an admin uploads
   to declare results. See *Result Tabulation Sheet* below.
3. **Pipe-delimited grammar** (`ResultExtractor`) — a synthetic fallback grammar
   kept for fixtures and tests. See *Pipe-delimited grammar* below.

---

## Internal Assessment format

One PDF holds **one college + one programme** and the internal marks of its
students across several papers. `InternalAssessmentExtractor` reads the text
PDFBox produces (with `setSortByPosition(true)`) line by line. The text looks
like:

(All values below are synthetic / placeholders — no real student data.)

```
Guru Gobind Singh Indraprastha University, Delhi
Internal Assessment for Examination JUNE, 2026
<INSTITUTE NAME> (999)
<PROGRAMME NAME> (020)
Paper Code : BCAP312      Paper Name : DATA VISUALIZATION & ANALYTICS LAB
BATCH SEM/ANNUAL PAPERID ENROLLMENT STUDENT NAME MAX MARKS MARKS
2023 6 098312 00199902023 AARAV SHARMA 40 37
...
Programme - BCA (020)
School/Institute - DIT (999)
```

Each data row is `BATCH  SEM  PAPERID(6)  ENROLLMENT(11)  NAME…  MAX  MARKS`:

- **PAPERID** (6 digits) is the subject key; the `Paper Code :` / `Paper Name :`
  header gives that paper its human code and title, and the row's `MAX` is the
  internal maximum (e.g. 40 or 100).
- **ENROLLMENT** (11 digits) encodes the **college code at positions 4–6** and the
  **programme code at positions 7–9**, so a student's college/programme is taken
  from the enrollment number itself and never depends on reading the (repeated)
  page headers.
- The last token is the internal mark, or `A`/`AB`/`ABSENT` for an absentee
  (stored as absent, with a NULL mark).
- The programme's short name (for the home page, e.g. `BCA`) comes from the
  `Programme - BCA (020)` footer; the institute's full name comes from the header.

Only internal marks are stored — no grade, SGPA or CGPA (those belong to the
end-semester result). Re-uploading the same sheet updates rows in place; a
different course's sheet is added alongside without touching the first.

`InternalFixtureGenerator` produces a synthetic sheet in exactly this layout
(two courses, a multi-word name, a 100-mark paper and an absence) for the demo
and tests — no real student data is ever committed.

---

## Result Tabulation Sheet

This is the **real end-semester gazette**. One PDF holds **one programme and one
semester**, with a *Scheme of Examinations* table at the top and then a result
block per institution. `GazetteResultExtractor` (marker `RESULT TABULATION SHEET`)
parses it.

(All values below are synthetic / placeholders — no real student data.)

### The scheme table

```
 (SCHEME OF EXAMINATIONS)
Scheme of Programme Code: 999   Programme Name: DEMO PROGRAMME   SchemeID: ...   Sem./Year: 01 SEMESTER
S.No. Paper ID Code Subject Credit Type Exam Mode Kind Minor Major Max. Marks Pass Marks
01 999101 DMO-101 DEMO THEORY ONE  4 THEORY UES COMPULSORY MANDATORY  25  75  100  40
04 999171 DMO-171 DEMO PROFICIENCY  1 PRACTICAL NUES COMPULSORY MANDATORY --  100  100  40
```

Per row: the 6-digit **Paper ID**, the human **Code**, the **Subject** name, the
**Credit** (first bare integer after the name), and the last four numeric columns
`Minor Major Max Pass`. `Minor` is the internal maximum (`--` means the paper has
no internal component). A semester may have **more than one** scheme table (many
electives); they are merged by paper id.

### A student

PDFBox with `setSortByPosition(true)` flattens each student's single visual row
into three text lines:

```
00199999924 999101(4) 999103(4) 999161(2) 999171(1)  11
AARAV DEMO
SID: 000000000001  20  53  22  60  35  50 -  84
SchemeID: ...
 1 73(A) 82(A+) 85(A+) 84(A+)
```

- **Anchor line** — the 11-digit **enrollment number**, then one
  `paperid(credits)` token per paper, then the credits secured (CS). Credits are
  read here, so electives (classmates sitting different papers) need no special
  handling. As in the internal format, the enrollment number encodes the college
  code at positions 4–6 and the programme code at positions 7–9.
- **SID line** — `internal external` marks **interleaved**, two tokens per paper.
  A `-`, `--` or `A` means that component is blank/absent (e.g. a NUES paper has
  no internal: `- 84`).
- **Totals line** — a leading serial, then one `total(grade)` token per paper, or
  a bare `A` for an absent paper. Parentheses tell the grade letter `A` (as in
  `73(A)`) apart from the absent marker `A`. A trailing `*` (e.g. `40(P)*`) marks
  grace marks; the printed total is kept as-is.

A printed grade is trusted (it already reflects grace marks); it is only derived
from the total when absent. Re-importing a **revised** gazette (which re-states
only a paper or two per student) upserts those papers and recomputes the
semester's SGPA from *all* of that semester's declared marks — it does **not**
wipe the student's other papers.

`GazetteResultExtractorTest` exercises this layout (normal, absent and grace-marks
students) entirely on synthetic data.

---

## Pipe-delimited grammar

The parser (`ResultExtractor`) reads the text that PDFBox extracts from a result
PDF **line by line**, keeping track of the current college, programme, semester
and student. Because it is purely sequential, a student's record may split
across pages without any problem.

Each meaningful line is **pipe-delimited** and begins with a tag. Lines without a
pipe (banners, page numbers, headings) are ignored.

| Tag | Fields | Meaning |
|-----|--------|---------|
| `SESSION`   | `SESSION\|<text>` | Exam session, e.g. `December 2024`. |
| `COLLEGE`   | `COLLEGE\|<code>\|<name>` | Sets the current institution. |
| `PROGRAMME` | `PROGRAMME\|<code>\|<name>\|<durationYears>\|<semester>` | Sets the current programme **and semester**. |
| `PAPER`     | `PAPER\|<paperId>\|<paperCode>\|<name>\|<credits>` | A paper in the current programme/semester scheme. |
| `STUDENT`   | `STUDENT\|<enrollmentNo>\|<name>` | Starts a student under the current college/programme. |
| `MARK`      | `MARK\|<paperId>\|<internal>\|<external>\|<total>\|<grade?>` | One paper's marks for the current student. |

Rules:

- A `MARK` whose `external` or `total` field is `AB` (or `ABSENT`) marks the
  student **absent** in that paper; internal/external/total are then stored as
  NULL and the paper is graded `F` (counts as a backlog).
- The `grade` field is optional. If present it is trusted; if blank the grade is
  derived from `total` using the `grade_scale` table.
- One PDF may hold **many** `COLLEGE` / `PROGRAMME` blocks and **both**
  semesters — this is the "all courses in one upload" case.

## Example

```
GURU GOBIND SINGH INDRAPRASTHA UNIVERSITY
CONSOLIDATED RESULT GAZETTE
SESSION|December 2024

COLLEGE|101|University School of Information Technology
PROGRAMME|020|B.Tech Computer Science|4|1
PAPER|99101|MA101|Mathematics-I|4
PAPER|99102|CS103|Programming Fundamentals|4
STUDENT|00101012024|AARAV SHARMA
MARK|99101|25|65|90|
MARK|99102|22|40|62|
STUDENT|00301012024|KABIR SINGH
MARK|99101|10|20|30|
MARK|99102|25|30|55|
```

`FixturePdfGenerator` produces a sample PDF in the grammar above so the whole
pipeline can be run and tested without a real PDF.

## Re-tuning if the gazette layout changes

Every parser is a single translation point into `ParsedResult`; everything
downstream — grading, SGPA/CGPA, DAOs, toppers, web UI — is unaffected. If the
university changes a layout:

1. Inspect `PdfReader.readText(file)` output to see exactly what text PDFBox
   produces for the new PDF.
2. Adjust **only** the matching extractor's regexes:
   `GazetteResultExtractor` for the end-semester gazette,
   `InternalAssessmentExtractor` for internal sheets.
