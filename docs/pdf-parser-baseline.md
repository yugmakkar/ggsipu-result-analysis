# PDF parser baseline

This baseline was established before changing parser logic or the database
schema. PDF checks used `PdfReader` plus the two extractors only; no `import`
command or database connection was used.

## Environment and build

- Project compiler release: Java 17; installed runtime: Java 22.0.1.
- PDF library: Apache PDFBox 3.0.8.
- `package` completed successfully with cached Maven 3.9.16 and cached
  dependencies. The suite ran 30 tests: 0 failures, 0 errors, 0 skipped.
- The checked Maven Wrapper command (`.\\mvnw.cmd package`) did not start in the
  sandbox because its PowerShell handoff failed with `icm : Cannot index into a
  null array`. Direct cached Maven required the command-local
  `-Dmaven.repo.local=C:\\Users\\user\\.m2\\repository` setting because the sandbox
  presents an unusable default Maven home (`C:\\.m2`).
- PDFBox completed all reads. It emitted environment warnings while scanning a
  malformed Windows font and could not write its font cache under `C:\\`; these
  warnings did not affect extraction or test status.

## Database and schema

The default runtime configuration is embedded H2 in MySQL-compatibility mode.
Production MySQL is configured through `config.properties` or `DB_URL`,
`DB_USER`, and `DB_PASSWORD`; the example URL is
`jdbc:mysql://localhost:3306/ipu_result?useSSL=false&serverTimezone=UTC`.

`sql/schema.sql` defines `college`, `programme`, `student`, `grade_scale`,
`subject`, `result_upload`, `marks`, `semester_result`, and `admin`.

## Parser behavior

`ResultExtractor` is the legacy end-semester parser. It accepts only the
synthetic, line-oriented pipe grammar (`SESSION|`, `COLLEGE|`, `PROGRAMME|`,
`PAPER|`, `STUDENT|`, and `MARK|`). `InternalAssessmentExtractor` handles the
current Internal Assessment layout and is selected by the import service when
its marker is present.

| PDF | PDFBox text | Legacy `ResultExtractor` | `InternalAssessmentExtractor` |
| --- | --- | --- | --- |
| `sample-result.pdf` | 1,109 characters; 0 pipe lines | empty: 0 colleges, 0 programmes, 0 subjects, 0 students, 0 marks | session `JUNE, 2026`; 1 college, 1 programme, 2 subjects, 8 students, 8 marks |
| `sample-bca.pdf` | 1,109 characters; 0 pipe lines | empty: 0 colleges, 0 programmes, 0 subjects, 0 students, 0 marks | session `JUNE, 2026`; 1 college, 1 programme, 2 subjects, 8 students, 8 marks |
| `sample-mca.pdf` | 1,012 characters; 0 pipe lines | empty: 0 colleges, 0 programmes, 0 subjects, 0 students, 0 marks | session `JUNE, 2026`; 1 college, 1 programme, 2 subjects, 6 students, 6 marks |

The actionable legacy-format baseline is therefore an expected empty result
for each available sample. The repository has no currently available
pipe-delimited legacy sample to exercise the successful legacy path. Any new
real gazette parser work should retain this record and add a representative
fixture/test before changing `ResultExtractor`.
