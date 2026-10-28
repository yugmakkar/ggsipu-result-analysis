# Security notes

The site exposes some confidential data (student marks), so the following
protections are built in. Each one is checked by the smoke test in
`scripts/smoke-test.sh`.

## What is protected

| Area | Measure | Where |
|------|---------|-------|
| SQL injection | Every query uses `PreparedStatement` with bound parameters. No SQL is built by string concatenation from user input. | all DAOs |
| Admin passwords | Stored as PBKDF2-HMAC-SHA256 (120k iterations, per-user salt). Never stored or logged in plaintext. Verified in constant time. | `PasswordHasher` |
| No default admin | The first admin is created explicitly (`create-admin`), with the password read from a prompt or `ADMIN_PASSWORD`. There is no built-in account. | `Main`, `AdminDAO` |
| Session fixation | `request.changeSessionId()` is called on successful login. | `LoginServlet` |
| Session cookies | `HttpOnly` (Tomcat default), 30-minute idle timeout. | `WebServer` |
| Brute force | Admin accounts lock for 15 minutes after 5 failed logins. | `AdminDAO` |
| CSRF | A per-session token is required on every POST (login, logout, upload). Missing/wrong token → `403`. | `Csrf`, servlets |
| Upload safety | Size limit, `%PDF-` magic-byte check, and a **server-generated** storage name (UUID) so a crafted filename cannot cause path traversal. Files are stored outside the webapp. | `UploadServlet` |
| Output escaping | All dynamic values (including student names and anything parsed from the PDF) are rendered through JSTL `<c:out>`, which HTML-escapes. | all JSPs |
| Response headers | `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Content-Security-Policy` (self only), `Referrer-Policy: no-referrer`. | `SecurityHeadersFilter` |
| Access control | `/admin/*` is behind an auth filter; unauthenticated requests are redirected to login. | `AuthFilter` |
| Result lookup | A public visitor looks up a result by **enrollment number alone** (no name), mirroring how GGSIPU results are themselves public. Until an end-semester result is declared, the page shows "result not declared yet" and no marks. See the trade-off below. | `ResultServlet` |
| Secrets | `config.properties` (real DB credentials) is gitignored; only `config.example.properties` is committed. | `.gitignore` |

## Known trade-off: public result lookup

The public result page (`/result`) shows a student's marksheet given their
**enrollment number alone** — no name or other factor is required. This is a
deliberate product choice: it matches how GGSIPU publishes results publicly, and
it keeps the student flow as simple as typing one number.

The trade-off is explicit: because enrollment numbers are sequential, anyone can
**walk the range** and read every declared student's marks, and the page does
reveal which enrollment numbers exist (a valid number returns a marksheet or
"not declared yet", an unknown one returns "no student on record"). Only
individual marks, SGPA and CGPA are exposed this way; admin management stays
behind login. If stricter privacy is ever wanted, put `/result` behind an auth
filter or require a second factor (e.g. date of birth) before showing a
marksheet — the lookup is the single place to change.

## Periodic checks

Re-run before each commit:

```bash
# No raw Statement-built SQL from user input, no unescaped EL output
grep -rn "createStatement" src/main/java        # only Db.runScript (schema) and count(table)
grep -rn "Statement " src/main/java | grep -iv prepared

# Then the live checks
bash scripts/smoke-test.sh
```
