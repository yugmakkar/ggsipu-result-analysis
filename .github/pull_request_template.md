## What this changes

A short description of the change and why.

Closes #<!-- issue number, if any -->

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Refactor / cleanup
- [ ] Documentation

## Checklist

- [ ] `./mvnw test` passes (all tests green)
- [ ] If the web layer changed, I ran `scripts/smoke-test.sh` against a running server
- [ ] Security protections kept intact: prepared statements, CSRF on POSTs, JSP output escaping, no secrets committed (`config.properties` stays gitignored)
- [ ] No real student data (names, enrollment numbers, marks, or real result PDFs) is included in the code, tests, or this PR
- [ ] If the PDF parser changed, I updated `docs/pdf-format.md`
- [ ] I created a branch for this change rather than committing to `master` directly

## Notes for the reviewer

Anything specific you'd like feedback on.
