#!/usr/bin/env bash
# Daily MySQL backup. Intended for cron, e.g.:
#   0 2 * * *  /opt/ipuresult/scripts/backup.sh >> /var/log/ipuresult-backup.log 2>&1
set -euo pipefail

DB_NAME="${DB_NAME:-ipu_result}"
DB_USER="${DB_USER:-ipu_app}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/ipuresult}"
KEEP_DAYS="${KEEP_DAYS:-14}"
# Password should come from ~/.my.cnf or the MYSQL_PWD env var, not the command line.

mkdir -p "$BACKUP_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$BACKUP_DIR/${DB_NAME}-${STAMP}.sql.gz"

echo "[$(date)] backing up $DB_NAME -> $OUT"
mysqldump --single-transaction --user="$DB_USER" "$DB_NAME" | gzip > "$OUT"

# prune old backups
find "$BACKUP_DIR" -name "${DB_NAME}-*.sql.gz" -mtime "+$KEEP_DAYS" -delete
echo "[$(date)] done; pruned backups older than ${KEEP_DAYS} days"
