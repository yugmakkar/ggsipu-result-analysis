# Result PDF formats

The app recognises two PDF layouts. The format is detected automatically on
upload (from the text PDFBox extracts) and routed to the matching parser.

1. **GGSIPU Internal Assessment sheet** (`InternalAssessmentExtractor`) — the
   real university format, used for the **Semester-1** showcase. This is what an
   admin uploads today. See *Internal Assessment format* below.
2. **End-semester grammar** (`ResultExtractor`) — a synthetic pipe-delimited
   grammar used by the dormant Semester-2 results/SGPA/topper path. See
   *End-semester grammar* below.

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

## End-semester grammar

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

## Adapting to a real GGSIPU gazette

A real gazette is laid out as multi-column tables, not this clean grammar.
When you have a real result PDF:

1. Run `java -jar result-analysis.jar` with a small throwaway call, or inspect
   the output of `PdfReader.readText(file)`, to see exactly what text PDFBox
   produces for your PDF.
2. Rewrite **only** `ResultExtractor.extract(...)` to recognise that layout
   (regular expressions over the extracted lines). Everything downstream — the
   grading, SGPA/CGPA, DAOs, toppers, web UI — stays the same, because the
   extractor is the single translation point into `ParsedResult`.

`FixturePdfGenerator` produces a sample PDF in the grammar above so the whole
pipeline can be run and tested before a real PDF is available.
