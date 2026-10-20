# Contributing

Thanks for helping out! This project is open source under the MIT license.

By participating, you agree to abide by our
[Code of Conduct](CODE_OF_CONDUCT.md).

## Get set up (only Java is required)

You do **not** need to install Maven — the project ships with the Maven Wrapper
(`mvnw`), which downloads the right Maven version automatically.

1. Install a JDK (17 or newer): https://adoptium.net/
2. Clone the repo:
   ```bash
   git clone https://github.com/yugmakkar/ggsipu-result-analysis.git
   cd ggsipu-result-analysis
   ```
3. Build and run the tests:
   - Windows (PowerShell): `.\mvnw.cmd clean package`
   - macOS / Linux / Git Bash: `./mvnw clean package`

## Run it

```bash
java -jar target/result-analysis.jar gen-fixture sample-result.pdf
java -jar target/result-analysis.jar create-admin admin
java -jar target/result-analysis.jar web      # http://localhost:8080
```

See the [README](README.md) for full usage.

## Making changes

- **Direct push:** ask the owner to add you as a collaborator
  (GitHub → repo → Settings → Collaborators), then push to a branch and open a
  pull request.
- **Or fork:** click *Fork*, push to your fork, and open a pull request against
  this repo.
- Please create a branch per change rather than committing to `master` directly.

## Before you open a pull request

- Run `./mvnw test` — all tests must pass.
- If you touch the web layer, also run `scripts/smoke-test.sh` (see
  [docs/SECURITY.md](docs/SECURITY.md)) with the server running.
- Keep the security protections in place: prepared statements, CSRF on POSTs,
  output escaping in JSP, and no secrets committed (`config.properties` is
  gitignored).
- The PDF parser lives in one place — `ResultExtractor`. If you adapt it for a
  real GGSIPU gazette, update `docs/pdf-format.md` too.
