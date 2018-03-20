#!/usr/bin/env bats

load ../utils/commands

@test "Check rds configs are listed" {
  for OUTPUT in $(list-rds | jq ' .[] | [to_entries[].key] == ["databaseType","clusterNames","validated","publicInAccount","hdpVersion","name","connectionURL","id","type","creationDate","properties"]');
  do
    echo $OUTPUT
    [[ "$OUTPUT" == "true" ]]
  done
 }

@test "Create rds" {
  OUTPUT=$(create-rds  --name testrds --rds-username testuser --rds-password password --url http://google.hu --driver org.postgresql.Driver --database-engine MYSQL --type HIVE 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')

  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["connectionDriver","connectionPassword","connectionURL","connectionUserName","databaseEngine","name","type"]') == true ]]
}

@test "Check rds delete" {
  OUTPUT=$(delete-rds  --name testrds 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"rds config deleted"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}