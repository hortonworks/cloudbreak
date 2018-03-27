#!/usr/bin/env bats

load ../utils/commands

@test "Check proxy configs are listed" {
  for OUTPUT in $(list-proxy | jq ' .[] | [to_entries[].key] == ["Name","Host","Port","Protocol","User"]');
  do
    echo $OUTPUT
    [[ "$OUTPUT" == "true" ]]
  done
 }

@test "Create proxy" {
  OUTPUT=$(create-proxy  --name testproxy --proxy-host testhost --proxy-port 1 --proxy-user test --proxy-password test 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')
  echo $OUTPUT
  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["name","password","protocol","serverHost","serverPort","userName"]') == true ]]
}

@test "Check proxy delete" {
  OUTPUT=$(delete-proxy  --name testproxy 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"proxy config deleted"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}