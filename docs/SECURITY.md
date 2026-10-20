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
| Result lookup gate | A public visitor must supply the enrollment number **and** a matching student name. "No such student" and "name does not match" return the identical message, so there is no enumeration oracle. A logged-in admin may look up by enrollment alone. | `ResultServlet` |
| Secrets | `config.properties` (real DB credentials) is gitignored; only `config.example.properties` is committed. | `.gitignore` |

## Known trade-off: public result lookup

The public result page (`/result`) requires **both** the enrollment number and
a matching student name before it shows a marksheet (case-insensitive,
whitespace-normalised full-name match). Because enrollment numbers are
sequential, requiring the name stops anyone from walking the range and dumping
every student's marks. "No such student" and "name does not match" return the
same message, so the page is not an oracle for which enrollment numbers exist.

The residual trade-off: someone who already knows a student's enrollment number
**and** their exact name as recorded can view that student's marks — this mirrors
how GGSIPU results are themselves public. If stricter privacy is wanted, put
`/result` behind the same auth filter as `/admin`, or require a second factor
(e.g. date of birth) before showing a marksheet. Individual marks are the only
data exposed; admin management stays behind login.

## Periodic checks

Re-run before each commit:

```bash
# No raw Statement-built SQL from user input, no unescaped EL output
grep -rn "createStatement" src/main/java        # only Db.runScript (schema) and count(table)
grep -rn "Statement " src/main/java | grep -iv prepared

# Then the live checks
bash scripts/smoke-test.sh
```
