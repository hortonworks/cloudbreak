#!/usr/bin/env bats

load ../utils/commands

@test "List RDS configs with response [Name,ConnectionURL,DatabaseEngine,Type,Driver]" {
  for OUTPUT in $(list-database | jq '.[] | [to_entries[].key] == ["Name","ConnectionURL","DatabaseEngine","Type","Driver"]');
  do
    echo $OUTPUT
    [[ "$OUTPUT" == "true" ]]
  done
}

@test "New RDS with message [create database took]" {
  OUTPUT=$(create-postgres-database --name testrds --db-username testuser --db-password testpassword --url "jdbc:postgresql://google.com/db" --type HIVE 2>&1 | tail -n 1 | head -n 1)

  echo $OUTPUT
  [[ "${OUTPUT}" == *"create database took"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check RDS delete with message [database configuration deleted]" {
  OUTPUT=$(delete-database --name testrds 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"database configuration deleted"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}
