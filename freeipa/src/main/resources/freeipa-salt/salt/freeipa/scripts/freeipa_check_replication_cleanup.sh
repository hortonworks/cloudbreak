#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# freeipa_check_replication_cleanup.sh
#
# Polls a FreeIPA node's 389-ds over LDAP until replication cleanup for a
# set of removed hosts has converged, then exits 0.  Fails fast with an
# actionable message on timeout.
#
# Required env vars:
#   FPW              – 389-ds Directory Manager password
#   TARGET_HOSTS     – comma-separated FQDNs of removed hosts (empty → exit 0)
#
# Optional env vars (with defaults):
#   LDAP_URI              (default: ldap://localhost)
#   TIMEOUT_SECONDS       (default: 600)
#   POLL_INTERVAL_SECONDS (default: 10)
# ---------------------------------------------------------------------------

LDAP_URI="${LDAP_URI:-ldap://localhost}"
: "${FPW:?FPW (Directory Manager password) is required}"
TARGET_HOSTS="${TARGET_HOSTS:-}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-600}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-10}"

if [[ -z "$TARGET_HOSTS" ]]; then
  echo "TARGET_HOSTS is empty — nothing to check, exiting successfully."
  exit 0
fi

# Run an ldapsearch and capture stdout+stderr.  Never let a nonzero exit code
# (e.g. "No such object"/32 when the task container does not yet exist) abort
# the script — callers inspect the output instead.
# Args: <scope> <base> <filter> [attrs...]  where scope is base|one|sub.
ldapq() {
  local scope="$1" base="$2" filter="$3"
  shift 3
  ldapsearch -x -o ldif-wrap=no -LLL \
    -H "$LDAP_URI" \
    -D "cn=Directory Manager" \
    -w "$FPW" \
    -b "$base" \
    -s "$scope" \
    "$filter" "$@" 2>&1 || true
}

# Build an LDAP filter for replication agreements that mention any of the
# removed hosts.  Input: comma-separated list in $TARGET_HOSTS.
build_host_filter() {
  local _host parts=()

  IFS=',' read -ra _hosts <<< "$TARGET_HOSTS"

  for _host in "${_hosts[@]}"; do
    _host="${_host# }"   # trim leading space
    _host="${_host% }"   # trim trailing space
    [[ -n "$_host" ]] && parts+=("(nsDS5ReplicaHost=${_host})")
  done

  if [[ ${#parts[@]} -eq 0 ]]; then
    echo "(objectclass=nsds5replicationagreement)"
    return
  fi

  if [[ ${#parts[@]} -eq 1 ]]; then
    echo "(&(objectclass=nsds5replicationagreement)${parts[0]})"
    return
  fi

  local p or_clause="(|"
  for p in "${parts[@]}"; do
    or_clause+="$p"
  done
  or_clause+=")"
  echo "(&(objectclass=nsds5replicationagreement)${or_clause})"
}

HOST_FILTER="$(build_host_filter)"

elapsed=0
while true; do
  pending_cleanallruv=""
  pending_agreements=""
  ldap_result=""

  # Check 1: in-flight CleanAllRUV (and abort-CleanAllRUV) tasks.
  # The task-type containers (cn=cleanallruv / cn=abort cleanallruv) are permanent
  # entries in 389-ds, so a subtree search would always match the container node
  # itself and report "active" forever.  Use onelevel scope so only genuine child
  # task *instances* count, and exclude finished tasks (those carry nsTaskExitCode)
  # so a lingering completed entry cannot block.  Empty result = converged.
  for task_base in "cn=cleanallruv,cn=tasks,cn=config" "cn=abort cleanallruv,cn=tasks,cn=config"; do
    ldap_result="$(ldapq one "$task_base" "(&(objectclass=*)(!(nsTaskExitCode=*)))")"
    if echo "$ldap_result" | grep -qi "^dn:"; then
      pending_cleanallruv="${pending_cleanallruv} [CleanAllRUV task still active under '${task_base}']"
    fi
  done

  # Check 2: lingering replication agreements for the removed hosts.
  ldap_result="$(ldapq sub "cn=mapping tree,cn=config" "$HOST_FILTER" nsDS5ReplicaHost dn)"
  if echo "$ldap_result" | grep -qi "^dn:"; then
    lingering="$(echo "$ldap_result" | grep -i "^nsDS5ReplicaHost:" | awk '{print $2}' | sort -u | tr '\n' ',' | sed 's/,$//')"
    pending_agreements=" [Lingering replication agreement(s) for host(s): ${lingering}]"
  fi

  if [[ -z "$pending_cleanallruv" && -z "$pending_agreements" ]]; then
    echo "SUCCESS: FreeIPA replication cleanup converged for '${TARGET_HOSTS}' on ${LDAP_URI} after ${elapsed}s."
    exit 0
  fi

  if [[ "$elapsed" -ge "$TIMEOUT_SECONDS" ]]; then
    # Emit the actionable diagnostic to stderr so the salt cmd.run surfaces it as the failure reason
    # (SaltErrorResolver pulls this command's Stderr; see orchestrator-salt stderrcommands.yaml).
    {
      echo "TIMEOUT after ${elapsed}s waiting for FreeIPA replication cleanup on ${LDAP_URI} for hosts '${TARGET_HOSTS}'."
      echo "Still pending:${pending_cleanallruv}${pending_agreements}"
      echo ""
      echo "Action items:"
      echo "  1. On ${LDAP_URI} inspect 'cn=cleanallruv,cn=tasks,cn=config' for stuck tasks:"
      echo "       ldapsearch -x -H ${LDAP_URI} -D 'cn=Directory Manager' -w '<password>'"
      echo "         -b 'cn=cleanallruv,cn=tasks,cn=config' -s sub '(objectclass=*)'"
      echo "  2. If a CleanAllRUV task is wedged, run:"
      echo "       ipa-replica-manage clean-dangling-ruv"
      echo "     or delete the stuck task entry directly:"
      echo "       ldapdelete -x -H ${LDAP_URI} -D 'cn=Directory Manager' -w '<password>' '<task_dn>'"
      echo "  3. Then retry the scaling operation."
    } >&2
    exit 1
  fi

  echo "Waiting for replication cleanup on ${LDAP_URI} (elapsed ${elapsed}s / ${TIMEOUT_SECONDS}s):${pending_cleanallruv}${pending_agreements}"
  sleep "$POLL_INTERVAL_SECONDS"
  elapsed=$(( elapsed + POLL_INTERVAL_SECONDS ))
done
