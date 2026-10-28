# Changelog

All notable changes to this project are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). This project has no
version tags yet, so entries are grouped by date, newest first.

## 2026-10-29

### Added
- **End-semester result gazette parsing.** A new `GazetteResultExtractor` reads
  the real GGSIPU *Result Tabulation Sheet* (detected by the
  `RESULT TABULATION SHEET` marker) alongside the existing internal-assessment
  and fixture parsers. It reads credits and the internal maximum from the
  *Scheme of Examinations* table, handles electives, absences and grace marks,
  sorts the marks into internal/external/total/grade columns, and computes SGPA
  and CGPA. Re-importing a revised gazette updates only its papers and
  recomputes the semester's SGPA without wiping the student's other papers.
- **Public demo admin.** The `web` command seeds a documented admin
  (`admin` / `admin12345`) when that username is absent, so a fresh clone can be
  logged into without running `create-admin`. Disable on a real deployment with
  `DEMO_ADMIN=false`. See [docs/SECURITY.md](docs/SECURITY.md).

### Changed
- **Result lookup is now by enrollment number alone** — the name field is gone
  from the student forms. Until a semester's end-semester result is declared the
  page shows "result not declared yet" and no marks; the home page shows the
  same when nothing has been declared. (Relaxes the name + enrollment gate added
  on 2026-10-20.)

## 2026-10-20

### Added
- **Internal Assessment PDF parsing (Semester 1).** `InternalAssessmentExtractor`
  reads the real GGSIPU internal-marks sheet and stores per-subject internal
  marks (no grade/SGPA, which belong to the end-semester result).
- GitHub community templates and a code of conduct.

### Changed
- Public result lookup required **name + enrollment number** as an
  anti-enumeration measure. _(Superseded on 2026-10-29 by enrollment-only
  lookup.)_

## 2026-10-15

### Added
- **Initial release** — GGSIPU result analysis and CGPA calculator. Web
  (Jakarta Servlets/JSP on embedded Tomcat) and console entry points; embedded
  H2 by default with a MySQL option; PDF import via Apache PDFBox; grade scale,
  SGPA and CGPA computation.
- **Single-college UI** — a data-driven courses list on the landing page;
  topper analyses present but under development and off by default.
- **Open source** — Maven wrapper, MIT license and contributor documentation.

### Security
- Prepared statements for every query, PBKDF2-hashed admin passwords, CSRF
  tokens on all POSTs, login lockout, upload validation (magic bytes, size
  limit, server-generated storage name), output escaping in JSPs, an auth filter
  on `/admin/*`, and gitignored real credentials.
