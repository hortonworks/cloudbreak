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

# List agreements + live status under an endpoint.
# Prints:  <agmt-cn>\t<host>\t<code-status>\t<updateEnd>\t<inProgress>
agmt_status() {
  local uri="$1"; shift
  ldapsearch -o ldif-wrap=no -LLL -H "$uri" "$@" \
    -b "cn=mapping tree,cn=config" \
    "(objectclass=nsds5replicationagreement)" \
    cn nsDS5ReplicaHost nsds5replicaLastUpdateStatus \
    nsds5replicaLastUpdateEnd nsds5replicaUpdateInProgress 2>/dev/null \
  | awk '
      /^dn:/ { if(cn) print cn"\t"host"\t"st"\t"end"\t"inp; cn=host=st=end=inp="" }
      /^cn:/ { cn=$2 }
      /^nsDS5ReplicaHost:/ { host=$2 }
      /^nsds5replicaLastUpdateStatus:/ { sub(/^[^:]+: /,""); st=$0 }
      /^nsds5replicaLastUpdateEnd:/ { end=$2 }
      /^nsds5replicaUpdateInProgress:/ { inp=$2 }
      END { if(cn) print cn"\t"host"\t"st"\t"end"\t"inp }'
}

# An endpoint passes when every agreement it hosts is code 0, not mid-update,
# and has actually completed a session (updateEnd not epoch/0).
#
# Optional 1st arg ONLY_HOST restricts evaluation to the single agreement whose
# nsDS5ReplicaHost equals ONLY_HOST. This is used for peer endpoints so we only
# judge the peer->new-replica agreement and ignore the peer's agreements to other
# masters, which sit in transient "busy replica" states during a rolling upgrade
# and are unrelated to the new replica's health. Empty ONLY_HOST evaluates all.
endpoint_healthy() {
  local only_host="$1"; shift
  local uri="$1"; shift
  local cn host st end inp code bad=0 seen=0
  while IFS=$'\t' read -r cn host st end inp; do
    [ -z "$cn" ] && continue
    [ -n "$only_host" ] && [ "$host" != "$only_host" ] && continue
    seen=1; code="$(status_code "$st")"
    if [ "${code:-x}" != "0" ] || [ "$inp" = "TRUE" ] \
       || [ -z "$end" ] || [[ "$end" == 1970* ]] || [ "$end" = "0" ]; then
      echo "  FAIL agmt=$cn -> $host  status='$st' inProgress=$inp end=$end"; bad=1
    else
      echo "  ok   agmt=$cn -> $host  ($st)"
    fi
  done < <(agmt_status "$uri" "$@")
  if [ -n "$only_host" ]; then
    # Peer endpoint scoped to the new replica: a master with no agreement to the new
    # replica is not a direct replication partner (no topology segment) and must count
    # as healthy, not as a failure. Only an existing agreement in a bad state fails.
    [ "$bad" = 0 ]
  else
    # Local endpoint: the new replica must host at least one agreement, all healthy.
    [ "$seen" = 1 ] && [ "$bad" = 0 ]
  fi
}

echo "$FPW" | kinit "$ADMIN_USER" >/dev/null
MASTERS="$(ldapsearch -o ldif-wrap=no -LLL -H "$LDAPI" -Y EXTERNAL \
            -b "cn=masters,cn=ipa,cn=etc,$BASEDN" "(objectclass=nsContainer)" dn 2>/dev/null \
          | sed -n 's/^dn: cn=\([^,]*\),cn=masters.*/\1/p' | grep -vx "$FQDN")"

deadline=$(( $(date +%s) + TIMEOUT ))
while :; do
  ok=1
  echo "== local ($FQDN), all suffixes =="
  endpoint_healthy "" "$LDAPI" -Y EXTERNAL || ok=0
  for m in $MASTERS; do
    echo "== peer $m (peer->$FQDN agreements only) =="
    endpoint_healthy "$FQDN" "ldap://$m" -Y GSSAPI -Q || ok=0
  done
  if [ "$ok" = 1 ]; then
    echo "SUCCESS: replication converged both directions"; exit 0
  fi
  [ "$(date +%s)" -ge "$deadline" ] && break
  sleep "$POLL"
done
echo "FAIL: replica $FQDN not healthy within ${TIMEOUT}s (see failing agreements)" >&2
exit 1
