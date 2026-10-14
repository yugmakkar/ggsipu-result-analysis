#!/usr/bin/env bash
# End-to-end + security smoke test against a running server.
# Prereqs:
#   - server running (java -jar target/result-analysis.jar web)
#   - an admin account created (java -jar target/result-analysis.jar create-admin admin)
#   - a sample PDF (java -jar target/result-analysis.jar gen-fixture sample.pdf)
#
# Usage:  BASE=http://localhost:8080 ADMIN_USER=admin ADMIN_PASS=... PDF=sample.pdf scripts/smoke-test.sh
set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-}"
PDF="${PDF:-sample.pdf}"
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

# login without CSRF -> 403
[[ "$(code -c "$J" -b "$J" -d "username=$ADMIN_USER&password=$ADMIN_PASS" $BASE/login)" == 403 ]] \
  && ok "login without CSRF is 403" || bad "login without CSRF is 403"

# get CSRF, then log in
TOK=$(curl -s -c "$J" -b "$J" "$BASE/login" | grep -oE 'name="csrf" value="[^"]+"' | sed -E 's/.*value="([^"]+)".*/\1/')
curl -s -o /dev/null -c "$J" -b "$J" --data-urlencode "csrf=$TOK" \
  -d "username=$ADMIN_USER&password=$ADMIN_PASS" "$BASE/login"
[[ "$(code -b "$J" $BASE/admin)" == 200 ]] && ok "login + dashboard" || bad "login + dashboard"

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

echo "----"
echo "PASS=$pass FAIL=$fail"
rm -f "$J"
exit $(( fail > 0 ? 1 : 0 ))
