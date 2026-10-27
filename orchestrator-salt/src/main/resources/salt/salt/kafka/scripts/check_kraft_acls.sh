#!/usr/bin/env bash

set -euo pipefail

CM_PROCESS_DIR=/var/run/cloudera-scm-agent/process
KAFKA_CLIENT_CONF=/etc/kafka/conf/kafka-client.conf
KAFKA_ACLS_TIMEOUT_SECONDS=120

# The failure reasons are written to stderr, because the script is registered in salt/stderrcommands.yaml, so Cloudbreak reports stderr as the error message
fail() {
  echo "$1" >&2
  exit 1
}

# Dedicated credential cache, otherwise the check would overwrite the default one of the broker node
work_dir=$(mktemp -d /tmp/kraft-acl-check.XXXXXX)
export KRB5CCNAME="$work_dir/krb5cc"
trap 'kdestroy > /dev/null 2>&1 || true; rm -rf "$work_dir"' EXIT

# CM keeps the process directories of the previous role starts, the newest one has the current keytab of the broker
broker_process_dir=$(ls -1dt "$CM_PROCESS_DIR"/*-KAFKA_BROKER 2> /dev/null | head -n 1 || true)
if [ -z "$broker_process_dir" ]; then
  fail "No Kafka broker process directory found under $CM_PROCESS_DIR, the KRaft cluster ACLs cannot be verified."
fi
kafka_keytab="$broker_process_dir/kafka.keytab"
if [ ! -f "$kafka_keytab" ]; then
  fail "No kafka.keytab found in $broker_process_dir, the KRaft cluster ACLs cannot be verified."
fi
echo "Using Kafka keytab $kafka_keytab"

keytab_entries=$(klist -kte "$kafka_keytab" 2>&1) || fail "Reading the keytab $kafka_keytab failed: $keytab_entries"
# The broker keytab contains other service principals too (e.g. cc_metric_reporter), only the kafka service principal may list the cluster ACLs
kafka_principal=$(awk '$4 ~ /^kafka\// {print $4; exit}' <<< "$keytab_entries")
if [ -z "$kafka_principal" ]; then
  fail "No kafka service principal found in $kafka_keytab, the KRaft cluster ACLs cannot be verified."
fi
echo "Using Kafka principal $kafka_principal"

kinit_output=$(kinit -kt "$kafka_keytab" "$kafka_principal" 2>&1) || fail "Kerberos login failed with the principal $kafka_principal: $kinit_output"

if [ ! -f "$KAFKA_CLIENT_CONF" ]; then
  fail "Kafka client configuration $KAFKA_CLIENT_CONF is missing, the KRaft cluster ACLs cannot be verified."
fi

bootstrap_server=$(awk -F'=' '/^bootstrap\.servers/ {print $2; exit}' "$KAFKA_CLIENT_CONF" | tr -d '[:space:]')
if [ -z "$bootstrap_server" ]; then
  fail "No bootstrap.servers entry in $KAFKA_CLIENT_CONF, the KRaft cluster ACLs cannot be verified."
fi
echo "Using bootstrap server $bootstrap_server"

# Lists the ACLs into acls_output, the extra arguments are passed to kafka-acls as resource filters.
# Only stdout holds the ACL listing, the client warnings on stderr could contain host names that match the principal by accident.
kafka_acls_stderr="$work_dir/kafka-acls.err"
list_acls() {
  set +e
  acls_output=$(timeout "$KAFKA_ACLS_TIMEOUT_SECONDS" kafka-acls --bootstrap-server "$bootstrap_server" --command-config "$KAFKA_CLIENT_CONF" \
    --list "$@" 2> "$kafka_acls_stderr")
  local list_acls_exit_code=$?
  set -e
  if [ "$list_acls_exit_code" -eq 124 ]; then
    fail "Listing the cluster ACLs on $bootstrap_server did not finish within $KAFKA_ACLS_TIMEOUT_SECONDS seconds."
  elif [ "$list_acls_exit_code" -ne 0 ]; then
    # The client logs its shutdown after the failure, so the error lines are picked instead of the end of the output
    local kafka_acls_errors
    kafka_acls_errors=$(grep -E 'ERROR|Exception|Error:' "$kafka_acls_stderr" | tail -n 5 || true)
    fail "Listing the cluster ACLs on $bootstrap_server failed with exit code $list_acls_exit_code: ${kafka_acls_errors:-$(tail -n 5 "$kafka_acls_stderr")}"
  fi
}

# Every resource block starts with a "Current ACLs for resource" header, only the entries of the CLUSTER resource blocks are counted.
# The principal type is written as User or USER depending on the authorizer, GROUP principals named kraft must not be counted, and DENY entries
# do not grant anything to the KRaft principal
count_kraft_cluster_acls() {
  awk '/^Current ACLs for resource/ {in_cluster = ($0 ~ /resourceType=CLUSTER[,)]/); next} in_cluster' <<< "$1" \
    | grep -iE 'principal=user:kraft([^[:alnum:]_.-]|$)' | grep -ciE 'permissionType=ALLOW([^[:alnum:]_]|$)' || true
}

# The Ranger authorizer answers the ACL filters differently per runtime: on 7.3.2 only the literal --cluster filter returns the cluster
# policies (resource name kafka-cluster), on 7.3.3 it returns nothing, because the cluster policies are reported with the name *, and
# only the match pattern type filter returns them (together with the ACLs of the other resource types)
list_acls --cluster
kraft_acl_count=$(count_kraft_cluster_acls "$acls_output")
if [ "$kraft_acl_count" -eq 0 ]; then
  echo "No cluster ACL found for the KRaft principal with the literal --cluster filter, checking with the match pattern type filter."
  list_acls --cluster --resource-pattern-type match
  kraft_acl_count=$(count_kraft_cluster_acls "$acls_output")
fi
if [ "$kraft_acl_count" -eq 0 ]; then
  fail "No cluster ACL belongs to the KRaft principal on $bootstrap_server. The Zookeeper to KRaft migration requires cluster ACLs for the KRaft principal, grant them and retry the migration."
fi

echo "Found $kraft_acl_count cluster ACL entries for the KRaft principal."
