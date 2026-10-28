#!/usr/bin/env bash
# End-to-end + security smoke test against a running server.
# Prereqs:
#   - server running (java -jar target/result-analysis.jar web)
#   - an admin account created (java -jar target/result-analysis.jar create-admin admin)
#   - a sample PDF (java -jar target/result-analysis.jar gen-fixture sample-bca.pdf bca)
#
# Usage:  BASE=http://localhost:8080 ADMIN_USER=admin ADMIN_PASS=... PDF=sample-bca.pdf scripts/smoke-test.sh
set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-}"
PDF="${PDF:-sample-bca.pdf}"
J="$(mktemp)"
pass=0; fail=0
ok()  { echo "  PASS  $1"; pass=$((pass+1)); }
bad() { echo "  FAIL  $1"; fail=$((fail+1)); }

code() { curl -s -o /dev/null -w "%{http_code}" "$@"; }

echo "Target: $BASE"

[[ "$(code $BASE/)" == 200 ]] && ok "home 200" || bad "home 200"

curl -s -D - -o /dev/null "$BASE/" | grep -qi "x-content-type-options: nosniff" \
  && ok "security headers present" || bad "security headers present"

[[ "$(code $BASE/admin)" == 302 ]] && ok "/admin redirects when unauthenticated" \
  || bad "/admin redirects when unauthenticated"
[[ "$(code $BASE/admin/students)" == 302 ]] && ok "/admin/students redirects when unauthenticated" \
  || bad "/admin/students redirects when unauthenticated"

# only the admin may upload: a multipart POST with no session is redirected, never imported
[[ "$(code -F "pdf=@$PDF;type=application/pdf" $BASE/admin/upload)" == 302 ]] \
  && ok "upload blocked when unauthenticated" || bad "upload blocked when unauthenticated"

# home shows Courses, not Colleges
curl -s "$BASE/" | grep -q "Courses" && ! curl -s "$BASE/" | grep -q "Colleges" \
  && ok "home shows Courses, not Colleges" || bad "home shows Courses, not Colleges"

# error pages must not leak the server version
curl -s "$BASE/does-not-exist" | grep -qi "Apache Tomcat" \
  && bad "error page leaks server info" || ok "error page hides server info"

# login without CSRF -> 403
[[ "$(code -c "$J" -b "$J" -d "username=$ADMIN_USER&password=$ADMIN_PASS" $BASE/login)" == 403 ]] \
  && ok "login without CSRF is 403" || bad "login without CSRF is 403"

# get CSRF, then log in
TOK=$(curl -s -c "$J" -b "$J" "$BASE/login" | grep -oE 'name="csrf" value="[^"]+"' | sed -E 's/.*value="([^"]+)".*/\1/')
curl -s -o /dev/null -c "$J" -b "$J" --data-urlencode "csrf=$TOK" \
  -d "username=$ADMIN_USER&password=$ADMIN_PASS" "$BASE/login"
[[ "$(code -b "$J" $BASE/admin)" == 200 ]] && ok "login + dashboard" || bad "login + dashboard"
[[ "$(code -b "$J" $BASE/admin/students)" == 200 ]] && ok "admin can list all students" \
  || bad "admin can list all students"

# upload needs a fresh token (session id rotated on login)
TOK=$(curl -s -b "$J" -c "$J" "$BASE/admin" | grep -oE 'name="csrf" value="[^"]+"' | head -1 | sed -E 's/.*value="([^"]+)".*/\1/')

# non-PDF rejected (relative path so native Windows curl can read it too)
NOTPDF="smoke-not-a-pdf.txt"
echo "not a pdf" > "$NOTPDF"
curl -s -b "$J" -F "csrf=$TOK" -F "pdf=@$NOTPDF;type=application/pdf" "$BASE/admin/upload" \
  | grep -q "not a PDF" && ok "non-PDF rejected" || bad "non-PDF rejected"
rm -f "$NOTPDF"

# upload without CSRF -> 403
[[ "$(code -b "$J" -F "pdf=@$PDF;type=application/pdf" $BASE/admin/upload)" == 403 ]] \
  && ok "upload without CSRF is 403" || bad "upload without CSRF is 403"

# real import
curl -s -b "$J" -F "csrf=$TOK" -F "pdf=@$PDF;type=application/pdf" "$BASE/admin/upload" \
  | grep -q "Imported" && ok "PDF imported" || bad "PDF imported"

# XSS: crafted enrollment must be escaped in the page
curl -s "$BASE/result?enroll=%3Cscript%3E" | grep -q "&lt;script&gt;" \
  && ok "output is HTML-escaped" || bad "output is HTML-escaped"

# result lookup is by enrollment number alone (no name required). The bca fixture
# student 00199902023 has only internal-assessment marks, so no end-semester
# result is declared for them: a public lookup must say so and must NOT leak marks.
curl -s "$BASE/result?enroll=00199902023" | grep -qi "not been declared" \
  && ok "internal-only lookup shows result-not-declared" || bad "internal-only lookup shows result-not-declared"
curl -s "$BASE/result?enroll=00199902023" | grep -qi "internal assessment" \
  && bad "internal marks must stay hidden from public before declaration" \
  || ok "internal marks hidden from public before declaration"

# toppers are under development: every type returns the notice and leaks no student data
for ttype in overall semester subject; do
  body="$(curl -s "$BASE/toppers?type=$ttype")"
  echo "$body" | grep -qi "under development" \
    && ! echo "$body" | grep -q "AARAV SHARMA" \
    && ok "toppers?type=$ttype under development, no data leaked" \
    || bad "toppers?type=$ttype under development, no data leaked"
done

echo "----"
echo "PASS=$pass FAIL=$fail"
rm -f "$J"
exit $(( fail > 0 ? 1 : 0 ))
