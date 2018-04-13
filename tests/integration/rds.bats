#!/usr/bin/env bats

load ../utils/commands

#@test "Check rds configs are listed" {
  #for OUTPUT in $(list-database | jq ' .[] | [to_entries[].key] == ["databaseType","clusterNames","validated","publicInAccount","hdpVersion","name","connectionURL","id","type","creationDate","properties"]');
  #do
    #echo $OUTPUT
    #[[ "$OUTPUT" == "true" ]]
  #done
 #}
#
#@test "Create rds" {
  #OUTPUT=$(create-database postgres  --name testrds --db-username testuser --db-password password --url jdbc:postgresql://google.com:1234/db --driver org.postgresql.Driver --database-engine MYSQL --type HIVE 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')
#
  #[[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["databaseType","clusterNames","validated","publicInAccount","hdpVersion","name","connectionURL","id","type","creationDate","properties"]') == true ]]
#
#}
#
@test "Check rds delete" {
  OUTPUT=$(delete-database  --name testrds 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"database configuration deleted"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}
