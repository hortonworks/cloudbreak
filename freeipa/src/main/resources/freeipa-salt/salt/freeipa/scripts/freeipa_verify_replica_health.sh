#!/usr/bin/env bash
set -uo pipefail

# ---------------------------------------------------------------------------
# freeipa_verify_replica_health.sh
#
# Exact, command-based replication-health gate for a FreeIPA master.  Exits 0
# only when this host has working, converged 389-ds replication (GSSAPI binds
# actually succeeding) on BOTH suffixes (dc=... and o=ipaca) and in BOTH
# directions (agreements hosted on this host AND the peer->this-host agreements
# hosted on every other master).
# An idle agreement, one that has not needed a session since dirsrv started,
# passes only when the same host pair proves both points on its behalf: see
# idle_agmts_corroborated.
# A failing status that proves the bind worked (busy replica, transient
# back-off, a session never ended) passes only when the consumer already holds
# every change of the supplier: see pending_agmts_converged.
#
# Unlike a client "kinit", this observes the operational attribute
# nsds5replicaLastUpdateStatus on each nsds5replicationagreement, which is the
# long-running dirsrv process' own last replication-bind result.  A poisoned
# dirsrv (latched onto the external ccache /tmp/krb5cc_389) surfaces here as
# "(-1) ... Can't contact LDAP server", and a broken GSSAPI replication bind as
# "(49) ... Invalid credentials", where a client kinit would falsely pass.
#
# Used on a freshly installed replica (post-install gate in
# freeipa_replica_install.sh) and on existing masters (post-promote gate and the
# post-downscale survivor convergence check).  It only observes: it never
# restarts, re-initializes or otherwise repairs anything.
#
# Fails closed: a peer that cannot be queried, a peer that hosts no agreement at
# all, or a local endpoint without agreements while other masters exist counts
# as UNHEALTHY, never as healthy.
#
# Runs LDAPI/EXTERNAL locally (no password); GSSAPI as admin for the remote
# masters, using a private ccache (never the shared /tmp/krb5cc_389 of ns-slapd).
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
#   EXCLUDE_HOSTS         (default: empty) - comma-separated FQDNs that are
#                         ignored both as peers and as agreement targets (hosts
#                         that were just removed from the topology)
#
# Exit codes:
#   0 - healthy
#   1 - unhealthy: at least one agreement reports a failure (non-zero code,
#       mid-update, ...), or this host's own endpoint could not be read or
#       hosts no agreement
#   3 - unhealthy, but no agreement is observed broken, and at least one
#       agreement hosted here completed a session, so this dirsrv's own
#       credentials work and restarting it would not help (it resets the
#       agreements to idle). What is left is any of: idle agreements (see
#       agmt_is_idle) that could not be corroborated, peers that cannot be
#       queried, peers that host no agreement, or a failed admin kinit (then
#       every peer query fails). Before exiting 3 the script prints one line
#       "RESULT: no agreement is broken; unresolved: <reason>; <reason>; ..."
#       on stdout naming them, so the caller does not have to guess.
#       When every agreement hosted here is idle, nothing proves that yet and
#       the exit code is 1.
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
EXCLUDE_HOSTS="${EXCLUDE_HOSTS:-}"

WORKDIR="$(mktemp -d /tmp/freeipa_replica_health.XXXXXX)"
# Private ccache for the admin GSSAPI queries: never touch root's default ccache
# or the dirsrv ccache.
export KRB5CCNAME="FILE:${WORKDIR}/krb5cc"
# Destroy the admin ticket explicitly (named ccache only, never the default),
# then drop the workdir.
trap 'kdestroy -c "$KRB5CCNAME" >/dev/null 2>&1; rm -rf "$WORKDIR"' EXIT

# Failure lines of the current poll; printed to stdout on timeout.
FAILS=()

# Reasons of the current poll that fail it without an agreement being seen
# broken (idle agreement not corroborated, peer not queryable, peer without
# agreements, failed kinit); printed in the RESULT line before exit 3.
UNRESOLVED=()

fail() { echo "  FAIL $*"; FAILS+=("$*"); }

unresolved() { UNRESOLVED+=("$*"); }

is_excluded() {
  local h
  [ -z "$EXCLUDE_HOSTS" ] && return 1
  IFS=',' read -ra _excluded <<<"$EXCLUDE_HOSTS"
  for h in "${_excluded[@]}"; do
    h="${h// /}"
    [ -n "$h" ] && [ "${h,,}" = "${1,,}" ] && return 0
  done
  return 1
}

# Extract the numeric code from an nsds5replicaLastUpdateStatus value, e.g.
#   "Error (0) Replica acquired successfully: Incremental update succeeded" -> 0
#   "Error (-1) Problem connecting to replica ... Can't contact LDAP server" -> -1
status_code() { sed -n 's/.*(\(-\?[0-9]\+\)).*/\1/p' <<<"$1" | head -1; }

# List the agreements hosted on an endpoint with their live status into
# $WORKDIR/agmts, one line per agreement:
#   <agmt-cn> <target-host> <suffix> <last-update-status> <last-update-end> <update-in-progress> <enabled>
# separated by \x1f (ASCII unit separator). Not a tab: tab is IFS whitespace,
# so 'read' would merge consecutive tabs and shift fields when one is empty.
# The same query returns the replica entries (cn=replica,...) with the RUV of
# each suffix; their elements go into $WORKDIR/ruv, one line each:
#   <suffix> <rid, or "gen" for the replica generation> <max CSN, may be empty>
# Returns the ldapsearch exit code; its stderr is kept in $WORKDIR/agmts.err.
agmt_status() {
  local uri="$1"; shift
  local rc
  ldapsearch -o ldif-wrap=no -LLL -H "$uri" "$@" \
    -b "cn=mapping tree,cn=config" \
    "(|(objectclass=nsds5replicationagreement)(objectclass=nsds5replica))" \
    cn nsDS5ReplicaHost nsDS5ReplicaRoot nsds5replicaLastUpdateStatus \
    nsds5replicaLastUpdateEnd nsds5replicaUpdateInProgress nsds5ReplicaEnabled nsds50ruv \
    >"$WORKDIR/agmts.ldif" 2>"$WORKDIR/agmts.err"
  rc=$?
  : >"$WORKDIR/ruv"
  awk -v ruvfile="$WORKDIR/ruv" '
      function emit(  i) {
        if (is_replica) {
          for (i = 1; i <= n; i++) print tolower(suffix)"\037"ruv_key[i]"\037"ruv_csn[i] > ruvfile
        } else if (agmt) {
          print agmt"\037"target"\037"suffix"\037"status"\037"update_end"\037"in_progress"\037"enabled
        }
      }
      { key = tolower($1) }
      key == "dn:" {
        emit(); agmt=target=suffix=status=update_end=in_progress=enabled=""; n=0
        is_replica = (tolower($0) ~ /^dn: cn=replica,/)
        next
      }
      # RUV element values: "{replicageneration} <csn>" or
      # "{replica <rid>[ ldap://<host>:<port>]}[ <min csn> <max csn>[ ...]]"
      is_replica && key == "nsds50ruv:" {
        if ($2 == "{replicageneration}") { ruv_key[++n] = "gen"; ruv_csn[n] = $3 }
        else if ($2 == "{replica") {
          rid = $3; sub(/}$/, "", rid)
          csns = $0; sub(/^[^}]*}/, "", csns); split(csns, csn_of, " ")
          ruv_key[++n] = rid; ruv_csn[n] = csn_of[2]
        }
        next
      }
      is_replica && key == "nsds5replicaroot:" { sub(/^[^:]+: /,""); suffix=$0; next }
      is_replica { next }
      key == "cn:" { agmt=$2 }
      key == "nsds5replicahost:" { target=$2 }
      key == "nsds5replicaroot:" { sub(/^[^:]+: /,""); suffix=$0 }
      key == "nsds5replicalastupdatestatus:" { sub(/^[^:]+: /,""); status=$0 }
      key == "nsds5replicalastupdateend:" { update_end=$2 }
      key == "nsds5replicaupdateinprogress:" { in_progress=$2 }
      key == "nsds5replicaenabled:" { enabled=$2 }
      END { emit() }' "$WORKDIR/agmts.ldif" >"$WORKDIR/agmts"
  return "$rc"
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

# True when a not-ok status still proves that the supplier bound to the consumer
# and acquired, or at least asked for, its replica: code 0 with a session
# started but not (yet) ended, a busy replica (the consumer answered the
# start-replication request, so the bind worked), or the transient back-off
# 389-ds reports when a session it acquired could not send its changes. 389-ds
# keeps such a status until the next session, and a supplier with nothing left
# to send starts none: after CleanAllRUV a supplier that still listed the
# removed replica fails once against an already cleaned consumer and then shows
# that failure for as long as its suffix sees no writes. Such an agreement is
# decided by pending_agmts_converged. Connection, credential and protocol
# errors never qualify, nor does a disabled agreement.
agmt_bind_proven() {
  local result_code="$1" status="$2" enabled="$3"
  { [ -z "$enabled" ] || [ "${enabled,,}" = "on" ]; } || return 1
  [[ "$status" == *"No replication sessions started since server startup"* ]] && return 1
  [ "$result_code" = "0" ] \
    || [[ "$status" == *"Can't acquire busy replica"* ]] \
    || [[ "$status" == *"Incremental update transient warning"* ]]
}

# Agreements seen in the current poll, one record each:
#   <supplier>\x1f<target>\x1f<suffix>\x1f<verdict: ok|idle|pending|bad>\x1f<agmt-cn>\x1f<last-update-status>
# Supplier, target and suffix are lower-cased: host names and DNs compare
# case-insensitively, and the hosts may not spell them the same way.
AGMTS=()

# Agreements of the current poll with verdict pending (see agmt_bind_proven):
#   <supplier>\x1f<target>\x1f<suffix>\x1f<agmt-cn>\x1f<update-in-progress>\x1f<last-update-end>\x1f<last-update-status>
PENDING=()

# Cleared by endpoint_healthy when an agreement of the current poll is seen
# broken (verdict bad); decides between exit codes 1 and 3.
NO_AGMT_BROKEN=1

# An endpoint passes when every agreement it hosts is code 0, not mid-update,
# and has actually completed a session (update end not epoch/0).  Agreements
# toward EXCLUDE_HOSTS are ignored.  Idle agreements (agmt_is_idle) are
# recorded and decided by idle_agmts_corroborated, pending ones
# (agmt_bind_proven) by pending_agmts_converged, once every endpoint has been
# read; they do not fail the endpoint here.  The RUVs the endpoint reports are
# kept in $WORKDIR/ruv.<supplier> for pending_agmts_converged.
#
# 1st arg ONLY_TARGET restricts evaluation to the agreements whose
# nsDS5ReplicaHost equals ONLY_TARGET. This is used for peer endpoints so we only
# judge the peer->this-host agreements and ignore the peer's agreements to other
# masters, which sit in transient "busy replica" states during a rolling upgrade
# and are checked on those masters themselves. Empty ONLY_TARGET evaluates all.
# 2nd arg SUPPLIER is the host the endpoint belongs to, i.e. the supplier of the
# agreements it hosts; also used in the failure lines.
# 3rd arg REQUIRE_LOCAL_AGMT (local endpoint only): 1 when at least one
# agreement must exist, i.e. other masters exist.
endpoint_healthy() {
  local only_target="$1" supplier="$2" require_agmt="$3"; shift 3
  local uri="$1"; shift
  local agmt target suffix status update_end in_progress enabled result_code verdict rc
  local any_bad=0 any_agmt_in_scope=0 total=0
  agmt_status "$uri" "$@"; rc=$?
  if [ "$rc" != 0 ]; then
    # An unreachable / unqueryable endpoint must never count as healthy.
    fail "on=$supplier query of $uri failed (ldapsearch rc=$rc): $(tr '\n' ' ' <"$WORKDIR/agmts.err")"
    [ -n "$only_target" ] && unresolved "peer $supplier could not be queried"
    return 1
  fi
  cp "$WORKDIR/ruv" "$WORKDIR/ruv.${supplier,,}"
  while IFS=$'\x1f' read -r agmt target suffix status update_end in_progress enabled; do
    [ -z "$agmt" ] && continue
    total=$((total + 1))
    is_excluded "$target" && continue
    [ -n "$only_target" ] && [ "${target,,}" != "${only_target,,}" ] && continue
    any_agmt_in_scope=1
    result_code="$(status_code "$status")"
    if agmt_is_idle "$result_code" "$status" "$update_end" "$in_progress" "$enabled"; then
      verdict=idle
      echo "  idle agmt=$agmt suffix=$suffix -> $target  ($status) - decided once all endpoints are read"
    elif [ "${result_code:-x}" != "0" ] || [ "$in_progress" = "TRUE" ] || no_session_ended "$update_end"; then
      if agmt_bind_proven "$result_code" "$status" "$enabled"; then
        verdict=pending
        echo "  pend agmt=$agmt suffix=$suffix -> $target  ($status) - decided against $target's RUV once all endpoints are read"
        PENDING+=("${supplier,,}"$'\x1f'"${target,,}"$'\x1f'"${suffix,,}"$'\x1f'"$agmt"$'\x1f'"$in_progress"$'\x1f'"$update_end"$'\x1f'"$status")
      else
        verdict=bad; any_bad=1; NO_AGMT_BROKEN=0
        fail "agmt=$agmt on=$supplier suffix=$suffix -> $target  status='$status' inProgress=$in_progress end=$update_end"
      fi
    else
      verdict=ok
      echo "  ok   agmt=$agmt suffix=$suffix -> $target  ($status)"
    fi
    AGMTS+=("${supplier,,}"$'\x1f'"${target,,}"$'\x1f'"${suffix,,}"$'\x1f'"$verdict"$'\x1f'"$agmt"$'\x1f'"$status")
  done <"$WORKDIR/agmts"
  if [ -n "$only_target" ]; then
    # Peer endpoint scoped to this host: a master with no agreement to this host
    # is not a direct replication partner (no topology segment) and counts as
    # healthy. A peer that hosts no agreement at all is not a working master of a
    # multi-master topology and fails, as does an existing agreement in a bad state.
    if [ "$total" = 0 ]; then
      fail "on=$supplier hosts no replication agreements at all"
      unresolved "peer $supplier hosts no replication agreement"
      return 1
    fi
    [ "$any_bad" = 0 ]
  else
    # Local endpoint: when other masters exist this host must host at least one
    # agreement, all healthy. A lone master has nothing to replicate with.
    if [ "$require_agmt" = 1 ] && [ "$any_agmt_in_scope" = 0 ]; then
      fail "on=$supplier hosts no replication agreement toward any other master"; return 1
    fi
    [ "$any_bad" = 0 ]
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
      fail "agmt=$agmt on=$supplier suffix=$suffix -> $target  status='$status' idle, not corroborated by a successful $supplier -> $target on another suffix and $target -> $supplier on $suffix"
      unresolved "idle agreement $agmt on $supplier -> $target ($suffix) not corroborated"
      all_corroborated=0
    fi
  done
  [ "$all_corroborated" = 1 ]
}

# ruv_covers SUPPLIER_RUV TARGET_RUV SUFFIX (RUV files written by agmt_status).
# True when TARGET's RUV on SUFFIX holds every change SUPPLIER's RUV on SUFFIX
# lists: the same replica generation, and for every replica ID with a max CSN
# on SUPPLIER the same ID with an equal or newer max CSN on TARGET. Then
# SUPPLIER has nothing left to send to TARGET on SUFFIX. Fails when either RUV
# was not read in this poll. A replica ID that SUPPLIER still lists and TARGET
# does not, e.g. one CleanAllRUV removed from TARGET only, fails it: SUPPLIER
# keeps trying to send its changes. CSNs are fixed-width hex, so they compare
# as strings (LC_ALL=C: byte order, "" forces string comparison in awk).
ruv_covers() {
  local supplier_ruv="$1" target_ruv="$2" suffix="$3"
  [ -f "$supplier_ruv" ] && [ -f "$target_ruv" ] || return 1
  LC_ALL=C awk -F '\037' -v suffix="$suffix" '
      FILENAME == ARGV[1] { if ($1 == suffix) { have[$2] = 1; csn[$2] = $3 }; next }
      $1 != suffix { next }
      { seen = 1 }
      $2 == "gen" { if (!($2 in have) || csn[$2] "" != $3 "") missing = 1; next }
      $3 == "" { next }
      !($2 in have) || csn[$2] "" < $3 "" { missing = 1 }
      END { exit (seen && !missing) ? 0 : 1 }' "$target_ruv" "$supplier_ruv"
}

# True when the RUV file holds an element of SUFFIX, i.e. the endpoint returned
# its cn=replica entry for SUFFIX.
ruv_read() {
  [ -f "$1" ] && awk -F '\037' -v suffix="$2" '$1 == suffix { found = 1; exit } END { exit !found }' "$1"
}

# Decide the pending agreements of this poll (see agmt_bind_proven): a pending
# agreement SUPPLIER -> TARGET on SUFFIX passes when TARGET already holds every
# change of SUPPLIER on SUFFIX (ruv_covers), i.e. its status is a leftover of a
# session that has nothing to redo. Otherwise it fails the poll and counts as
# broken. TARGET's RUV must be read after SUPPLIER's, or changes still in flight
# would look missing: peers are read after this host, so this host's RUV is read
# again when it is the target, into its own file, as the first read stays the
# supplier side of this host's agreements. The RUV is the one dirsrv returns on
# the cn=replica entry, built from its in-memory RUV on every search (the
# database tombstone copy is only written periodically). A converged pending
# agreement does not corroborate idle ones, as only a session that ended
# successfully in this poll proves the bind. Returns non-zero when any pending
# agreement fails.
pending_agmts_converged() {
  local record supplier target suffix agmt in_progress update_end status supplier_ruv target_ruv reason all_converged=1
  local local_target_ruv="$WORKDIR/ruv.${FQDN,,}.as-target"
  [ ${#PENDING[@]} = 0 ] && return 0
  echo "== pending agreements =="
  for record in "${PENDING[@]}"; do
    IFS=$'\x1f' read -r _ target _ <<<"$record"
    if [ "$target" = "${FQDN,,}" ]; then
      agmt_status "$LDAPI" -Y EXTERNAL && cp "$WORKDIR/ruv" "$local_target_ruv"
      break
    fi
  done
  for record in "${PENDING[@]}"; do
    IFS=$'\x1f' read -r supplier target suffix agmt in_progress update_end status <<<"$record"
    supplier_ruv="$WORKDIR/ruv.$supplier" target_ruv="$WORKDIR/ruv.$target"
    [ "$target" = "${FQDN,,}" ] && target_ruv="$local_target_ruv"
    if ruv_covers "$supplier_ruv" "$target_ruv" "$suffix"; then
      echo "  ok   agmt=$agmt on=$supplier suffix=$suffix -> $target  status '$status' is stale, $target already holds every change in $supplier's RUV"
    else
      if ! ruv_read "$supplier_ruv" "$suffix"; then
        reason="no RUV of $suffix could be read from $supplier"
      elif ! ruv_read "$target_ruv" "$suffix"; then
        reason="no RUV of $suffix could be read from $target"
      else
        reason="$target does not hold every change in $supplier's RUV"
      fi
      NO_AGMT_BROKEN=0
      fail "agmt=$agmt on=$supplier suffix=$suffix -> $target  status='$status' inProgress=$in_progress end=$update_end, and $reason"
      all_converged=0
    fi
  done
  [ "$all_converged" = 1 ]
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

# Other (not excluded) masters from cn=masters into $MASTERS; non-zero when the
# local query fails.
list_masters() {
  local m out rc
  out="$(ldapsearch -o ldif-wrap=no -LLL -H "$LDAPI" -Y EXTERNAL \
          -b "cn=masters,cn=ipa,cn=etc,$BASEDN" -s one "(objectclass=nsContainer)" dn 2>"$WORKDIR/masters.err")"
  rc=$?
  if [ "$rc" != 0 ]; then
    fail "on=$FQDN listing cn=masters failed (ldapsearch rc=$rc): $(tr '\n' ' ' <"$WORKDIR/masters.err")"
    return 1
  fi
  MASTERS=""
  while IFS= read -r m; do
    [ -z "$m" ] || [ "${m,,}" = "${FQDN,,}" ] && continue
    is_excluded "$m" && continue
    MASTERS="$MASTERS $m"
  done < <(sed -n 's/^dn: cn=\([^,]*\),cn=masters.*/\1/p' <<<"$out")
}

deadline=$(( $(date +%s) + TIMEOUT ))
while :; do
  # Two outcomes per poll: whether the poll is healthy at all, and whether any
  # agreement is seen broken (NO_AGMT_BROKEN, which decides the exit code).
  ok=1
  NO_AGMT_BROKEN=1
  FAILS=()
  UNRESOLVED=()
  AGMTS=()
  PENDING=()
  MASTERS=""
  rm -f "$WORKDIR"/ruv.*
  if ! klist -s >/dev/null 2>&1; then
    echo "$FPW" | kinit "$ADMIN_USER" >/dev/null 2>"$WORKDIR/kinit.err" \
      || { fail "on=$FQDN kinit as $ADMIN_USER failed: $(tr '\n' ' ' <"$WORKDIR/kinit.err")"
           unresolved "kinit as $ADMIN_USER failed, so no peer could be queried"; }
  fi
  if list_masters; then
    echo "== local ($FQDN), all suffixes; other masters:${MASTERS:- none}${EXCLUDE_HOSTS:+ (excluded: $EXCLUDE_HOSTS)} =="
    endpoint_healthy "" "$FQDN" "$([ -n "$MASTERS" ] && echo 1 || echo 0)" "$LDAPI" -Y EXTERNAL || ok=0
    for master in $MASTERS; do
      echo "== peer $master (peer->$FQDN agreements only) =="
      endpoint_healthy "$FQDN" "$master" 0 "ldap://$master" -Y GSSAPI -Q || ok=0
    done
    pending_agmts_converged || ok=0
    echo "== idle agreements =="
    idle_agmts_corroborated || ok=0
  else
    ok=0
  fi
  if [ "$ok" = 1 ]; then
    echo "SUCCESS: replication converged both directions on $FQDN"; exit 0
  fi
  [ "$(date +%s)" -ge "$deadline" ] && break
  sleep "$POLL"
done
# Engineer-facing detail goes to stdout (collected node logs). stderr carries a
# single summary line only: callers such as freeipa_replica_install.sh surface
# their stderr to the end user as the failure reason.
echo "Failing agreements / endpoints on $FQDN after ${TIMEOUT}s:"
for f in ${FAILS[@]+"${FAILS[@]}"}; do
  echo "  $f"
done
# Keep "not healthy within": VerifyReplicationCleanupService matches it to tell this timeout from a run that never got here.
echo "FAIL: replica $FQDN not healthy within ${TIMEOUT}s (see failing agreements)" >&2
# In the last poll no agreement was seen broken, and this host's dirsrv has
# proven it can bind outward: name what is left, then exit 3.
if [ "$NO_AGMT_BROKEN" = 1 ] && local_agmt_ok; then
  reasons=""
  for r in ${UNRESOLVED[@]+"${UNRESOLVED[@]}"}; do
    reasons="${reasons:+$reasons; }$r"
  done
  echo "RESULT: no agreement is broken; unresolved: ${reasons:-unknown}"
  exit 3
fi
exit 1
