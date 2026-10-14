# Result PDF format (parser grammar)

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
