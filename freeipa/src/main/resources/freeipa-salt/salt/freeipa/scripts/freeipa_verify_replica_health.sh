#!/usr/bin/env bash
set -uo pipefail

# ---------------------------------------------------------------------------
# freeipa_verify_replica_health.sh
#
# Exact, command-based replication-health gate for a freshly installed FreeIPA
# replica.  Exits 0 only when the new replica has working, converged 389-ds
# replication (GSSAPI binds actually succeeding) on BOTH suffixes
# (dc=... and o=ipaca) and in BOTH directions (agreements hosted on the new
# replica AND the peer->new agreements hosted on every existing master).
# An idle agreement, one that has not needed a session since dirsrv started,
# passes only when the same host pair proves both points on its behalf: see
# idle_agmts_corroborated.
#
# Unlike a client "kinit", this observes the operational attribute
# nsds5replicaLastUpdateStatus on each nsds5replicationagreement, which is the
# long-running dirsrv process' own last replication-bind result.  A poisoned
# dirsrv (latched onto the external ccache /tmp/krb5cc_389) surfaces here as
# "(-1) ... Can't contact LDAP server", where a client kinit would falsely pass.
#
# Runs on the new replica.  LDAPI/EXTERNAL locally (no password); GSSAPI as
# admin for the remote masters.
#
# Required env vars:
#   REALM        - Kerberos realm (e.g. EXAMPLE.COM); also the 389-ds instance
#                  name with '.' -> '-' (slapd-EXAMPLE-COM)
#   DOMAIN       - IPA domain (e.g. example.com); used to build the base DN
#   ADMIN_USER   - IPA admin principal used for the GSSAPI (remote) queries
#   FPW          - password for ADMIN_USER
#
# Optional env vars (with defaults):
#   TIMEOUT_SECONDS       (default: 300)
#   POLL_INTERVAL_SECONDS (default: 10)
#
# Exit codes:
#   0 - healthy
#   1 - unhealthy: at least one agreement reports a failure (non-zero code,
#       mid-update, ...) or the local endpoint hosts no agreement
#   3 - unhealthy, but no agreement is observed broken: the only ones left are
#       idle (see agmt_is_idle) and could not be corroborated, and at least one
#       agreement hosted here completed a session, so this dirsrv's own
#       credentials work and restarting it would not help (it resets the
#       agreements to idle). When every agreement hosted here is idle, nothing
#       proves that yet and the exit code is 1.
# ---------------------------------------------------------------------------

: "${REALM:?REALM is required}"
: "${DOMAIN:?DOMAIN is required}"
: "${ADMIN_USER:?ADMIN_USER is required}"
: "${FPW:?FPW (admin password) is required}"

FQDN="$(hostname -f)"
INST="$(echo "$REALM" | tr '.' '-')"                     # EXAMPLE.COM -> EXAMPLE-COM
LDAPI="ldapi://%2Frun%2Fslapd-${INST}.socket"
BASEDN="dc=${DOMAIN//./,dc=}"
TIMEOUT="${TIMEOUT_SECONDS:-300}"
POLL="${POLL_INTERVAL_SECONDS:-10}"

# Extract the numeric code from an nsds5replicaLastUpdateStatus value, e.g.
#   "Error (0) Replica acquired successfully: Incremental update succeeded" -> 0
#   "Error (-1) Problem connecting to replica ... Can't contact LDAP server" -> -1
status_code() { sed -n 's/.*(\(-\?[0-9]\+\)).*/\1/p' <<<"$1" | head -1; }

# List the agreements hosted on an endpoint with their live status.
# Prints one line per agreement:
#   <agmt-cn> <target-host> <suffix> <last-update-status> <last-update-end> <update-in-progress> <enabled>
# separated by \x1f (ASCII unit separator). Not a tab: tab is IFS whitespace,
# so 'read' would merge consecutive tabs and shift fields when one is empty.
agmt_status() {
  local uri="$1"; shift
  ldapsearch -o ldif-wrap=no -LLL -H "$uri" "$@" \
    -b "cn=mapping tree,cn=config" \
    "(objectclass=nsds5replicationagreement)" \
    cn nsDS5ReplicaHost nsDS5ReplicaRoot nsds5replicaLastUpdateStatus \
    nsds5replicaLastUpdateEnd nsds5replicaUpdateInProgress nsds5ReplicaEnabled 2>/dev/null \
  | awk '
      function emit() { if (agmt) print agmt"\037"target"\037"suffix"\037"status"\037"update_end"\037"in_progress"\037"enabled }
      { key = tolower($1) }
      key == "dn:" { emit(); agmt=target=suffix=status=update_end=in_progress=enabled="" }
      key == "cn:" { agmt=$2 }
      key == "nsds5replicahost:" { target=$2 }
      key == "nsds5replicaroot:" { sub(/^[^:]+: /,""); suffix=$0 }
      key == "nsds5replicalastupdatestatus:" { sub(/^[^:]+: /,""); status=$0 }
      key == "nsds5replicalastupdateend:" { update_end=$2 }
      key == "nsds5replicaupdateinprogress:" { in_progress=$2 }
      key == "nsds5replicaenabled:" { enabled=$2 }
      END { emit() }'
}

# True when no replication session has ended yet (no end time, or the epoch).
no_session_ended() {
  local update_end="$1"
  [ -z "$update_end" ] || [[ "$update_end" == 1970* ]] || [ "$update_end" = "0" ]
}

# True when the agreement has never started a session since dirsrv started:
# code 0 with 389-ds' initial "No replication sessions started since server
# startup" status, not mid-update and no session end time. 389-ds keeps this
# status in memory and overwrites it on the first acquire attempt, whatever the
# outcome (a bind failure, an unreachable consumer or a busy replica all show
# up with their own non-zero code instead). A supplier only acquires its
# consumer when it has changes to send, so an agreement on a suffix that sees
# no writes, typically o=ipaca on a freshly installed replica or right after
# a dirsrv restart, can stay in this state for a long time. It is not a
# failure, but it does not prove anything either: see idle_agmts_corroborated.
# A disabled agreement (nsds5ReplicaEnabled other than on/absent) never starts
# a session either, so it is never idle.
agmt_is_idle() {
  local result_code="$1" status="$2" update_end="$3" in_progress="$4" enabled="$5"
  [ "$result_code" = "0" ] && [ "$in_progress" = "FALSE" ] \
    && { [ -z "$enabled" ] || [ "${enabled,,}" = "on" ]; } \
    && [[ "$status" == *"No replication sessions started since server startup"* ]] \
    && no_session_ended "$update_end"
}

# Agreements seen in the current poll, one record each:
#   <supplier>\x1f<target>\x1f<suffix>\x1f<verdict: ok|idle|bad>\x1f<agmt-cn>\x1f<last-update-status>
# Supplier, target and suffix are lower-cased: host names and DNs compare
# case-insensitively, and the hosts may not spell them the same way.
AGMTS=()

# An endpoint passes when every agreement it hosts is code 0, not mid-update,
# and has actually completed a session (update end not epoch/0). Idle agreements
# (agmt_is_idle) are recorded and decided by idle_agmts_corroborated once every
# endpoint has been read; they do not fail the endpoint here.
#
# 1st arg ONLY_TARGET restricts evaluation to the agreements whose
# nsDS5ReplicaHost equals ONLY_TARGET. This is used for peer endpoints so we only
# judge the peer->new-replica agreements and ignore the peer's agreements to other
# masters, which sit in transient "busy replica" states during a rolling upgrade
# and are unrelated to the new replica's health. Empty ONLY_TARGET evaluates all.
# 2nd arg SUPPLIER is the host the endpoint belongs to, i.e. the supplier of the
# agreements it hosts.
endpoint_healthy() {
  local only_target="$1" supplier="$2"; shift 2
  local uri="$1"; shift
  local agmt target suffix status update_end in_progress enabled result_code verdict
  local any_bad=0 any_agmt_in_scope=0
  while IFS=$'\x1f' read -r agmt target suffix status update_end in_progress enabled; do
    [ -z "$agmt" ] && continue
    [ -n "$only_target" ] && [ "${target,,}" != "${only_target,,}" ] && continue
    any_agmt_in_scope=1
    result_code="$(status_code "$status")"
    if agmt_is_idle "$result_code" "$status" "$update_end" "$in_progress" "$enabled"; then
      verdict=idle
      echo "  idle agmt=$agmt suffix=$suffix -> $target  ($status) - decided once all endpoints are read"
    elif [ "${result_code:-x}" != "0" ] || [ "$in_progress" = "TRUE" ] || no_session_ended "$update_end"; then
      verdict=bad; any_bad=1
      echo "  FAIL agmt=$agmt suffix=$suffix -> $target  status='$status' inProgress=$in_progress end=$update_end"
    else
      verdict=ok
      echo "  ok   agmt=$agmt suffix=$suffix -> $target  ($status)"
    fi
    AGMTS+=("${supplier,,}"$'\x1f'"${target,,}"$'\x1f'"${suffix,,}"$'\x1f'"$verdict"$'\x1f'"$agmt"$'\x1f'"$status")
  done < <(agmt_status "$uri" "$@")
  if [ -n "$only_target" ]; then
    # Peer endpoint scoped to the new replica: a master with no agreement to the new
    # replica is not a direct replication partner (no topology segment) and must count
    # as healthy, not as a failure. Only an existing agreement in a bad state fails.
    [ "$any_bad" = 0 ]
  else
    # Local endpoint: the new replica must host at least one agreement, all healthy.
    [ "$any_agmt_in_scope" = 1 ] && [ "$any_bad" = 0 ]
  fi
}

# True when an agreement SUPPLIER -> TARGET with verdict ok (a completed,
# successful session) was seen in this poll on SUFFIX (agmt_ok_on_suffix) or on
# any suffix other than SUFFIX (agmt_ok_on_other_suffix). Idle agreements never
# count, so one idle agreement can never vouch for another.
agmt_ok_on_suffix() {
  local want_supplier="$1" want_target="$2" want_suffix="$3"
  local record supplier target suffix verdict
  for record in ${AGMTS[@]+"${AGMTS[@]}"}; do
    IFS=$'\x1f' read -r supplier target suffix verdict _ <<<"$record"
    [ "$verdict" = ok ] && [ "$supplier" = "$want_supplier" ] && [ "$target" = "$want_target" ] \
      && [ "$suffix" = "$want_suffix" ] && return 0
  done
  return 1
}

agmt_ok_on_other_suffix() {
  local want_supplier="$1" want_target="$2" other_than_suffix="$3"
  local record supplier target suffix verdict
  for record in ${AGMTS[@]+"${AGMTS[@]}"}; do
    IFS=$'\x1f' read -r supplier target suffix verdict _ <<<"$record"
    [ "$verdict" = ok ] && [ "$supplier" = "$want_supplier" ] && [ "$target" = "$want_target" ] \
      && [ -n "$suffix" ] && [ "$suffix" != "$other_than_suffix" ] && return 0
  done
  return 1
}

# Decide the idle agreements of this poll. An idle agreement SUPPLIER -> TARGET on
# SUFFIX passes only when BOTH of these were seen with a completed, successful
# session in the same poll:
#   - SUPPLIER -> TARGET on another suffix: the same dirsrv process (same
#     Kerberos credentials, same consumer host) can bind to TARGET and
#     replicate, so the idle one is not hiding a broken bind or an unreachable
#     consumer;
#   - TARGET -> SUPPLIER on the same suffix: the suffix is replicated between
#     the two hosts and a session on it works.
# Anything less (the other direction or the other suffix idle, failing, or not
# queryable, or an unknown suffix) fails the poll. Returns non-zero when any idle
# agreement is not corroborated.
idle_agmts_corroborated() {
  local record supplier target suffix verdict agmt status all_corroborated=1
  for record in ${AGMTS[@]+"${AGMTS[@]}"}; do
    IFS=$'\x1f' read -r supplier target suffix verdict agmt status <<<"$record"
    [ "$verdict" = idle ] || continue
    if [ -n "$suffix" ] && agmt_ok_on_other_suffix "$supplier" "$target" "$suffix" \
       && agmt_ok_on_suffix "$target" "$supplier" "$suffix"; then
      echo "  ok   agmt=$agmt on=$supplier suffix=$suffix -> $target  idle, corroborated by $supplier -> $target on another suffix and $target -> $supplier on $suffix"
    else
      echo "  FAIL agmt=$agmt on=$supplier suffix=$suffix -> $target  status='$status' idle, not corroborated by a successful $supplier -> $target on another suffix and $target -> $supplier on $suffix"
      all_corroborated=0
    fi
  done
  [ "$all_corroborated" = 1 ]
}

# True when an agreement hosted on this host completed a successful session in
# this poll, i.e. this dirsrv process has bound to a consumer with its own
# Kerberos credentials since it started. Idle agreements prove nothing about
# that: a dirsrv latched onto a stale ccache only shows it on its first acquire.
local_agmt_ok() {
  local record supplier verdict
  for record in ${AGMTS[@]+"${AGMTS[@]}"}; do
    IFS=$'\x1f' read -r supplier _ _ verdict _ <<<"$record"
    [ "$verdict" = ok ] && [ "$supplier" = "${FQDN,,}" ] && return 0
  done
  return 1
}

echo "$FPW" | kinit "$ADMIN_USER" >/dev/null
MASTERS="$(ldapsearch -o ldif-wrap=no -LLL -H "$LDAPI" -Y EXTERNAL \
            -b "cn=masters,cn=ipa,cn=etc,$BASEDN" "(objectclass=nsContainer)" dn 2>/dev/null \
          | sed -n 's/^dn: cn=\([^,]*\),cn=masters.*/\1/p' | grep -vx "$FQDN")"

deadline=$(( $(date +%s) + TIMEOUT ))
while :; do
  # Two independent outcomes per poll: whether any agreement or endpoint is seen
  # broken, and whether every idle agreement could be corroborated.
  no_agmt_broken=1; idle_corroborated=1
  AGMTS=()
  echo "== local ($FQDN), all suffixes =="
  endpoint_healthy "" "$FQDN" "$LDAPI" -Y EXTERNAL || no_agmt_broken=0
  for master in $MASTERS; do
    echo "== peer $master (peer->$FQDN agreements only) =="
    endpoint_healthy "$FQDN" "$master" "ldap://$master" -Y GSSAPI -Q || no_agmt_broken=0
  done
  echo "== idle agreements =="
  idle_agmts_corroborated || idle_corroborated=0
  if [ "$no_agmt_broken" = 1 ] && [ "$idle_corroborated" = 1 ]; then
    echo "SUCCESS: replication converged both directions"; exit 0
  fi
  [ "$(date +%s)" -ge "$deadline" ] && break
  sleep "$POLL"
done
echo "FAIL: replica $FQDN not healthy within ${TIMEOUT}s (see failing agreements)" >&2
# In the last poll nothing was seen broken, only idle agreements were left
# uncorroborated, and this host's dirsrv has proven it can bind outward.
[ "$no_agmt_broken" = 1 ] && local_agmt_ok && exit 3
exit 1
