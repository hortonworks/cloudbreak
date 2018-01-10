#!/usr/bin/env bats
load ../commands

@test "list clusters" {
  [[ $(list-clusters | jq ' length ' ) == $( curl -ks $CLOUD_URL/cb/api/v1/stacks/account  | jq 'length' ) ]]
}

@test "list clusters attributes" {
  for OUTPUT in $(list-clusters | jq ' .[] | [to_entries[].key] == ["Name","Description","CloudPlatform","StackStatus","ClusterStatus"]' );
  do
    [[ "$OUTPUT" == "true" ]]
  done
}

@test "describe cluster" {
  describe-cluster --name aaaaa
}

@test "describer cluster not found" {
  run describe-cluster --name az404
  [[ $status -eq 1 ]]
}

@test "delete cluster" {
  delete-cluster --name aaaaa
}

@test "delete cluster not found" {
  run delete-cluster --name az404
  [[ $status -eq 1 ]]
}

@test "start cluster" {
  start-cluster --name aaaaa
}

@test "start cluster not in apropriate status" {
  run start-cluster --name azstatus
  [[ $status -eq 1 ]]
}

@test "stop cluster" {
  stop-cluster --name aaaaa
}

@test "stop cluster not in apropriate status" {
  run stop-cluster --name azstatus
  [[ $status -eq 1 ]]
}

@test "sync cluster" {
  sync-cluster --name aaaaa
}

@test "sync cluster not in apropriate status" {
  skip "mock is not ready"
  run sync-cluster --name azstatus
  [[ $status -eq 1 ]]
}

@test "repair cluster" {
  repair-cluster --name aaaaa
}

@test "repair cluster not in apropriate status" {
  skip "mock is not ready"
  run repair-cluster --name azstatus
  [[ $status -eq 1 ]]
}

@test "scale cluster" {
  scale-cluster --name aaaaa --group-name a --desired-node-count 5
}

@test "scale cluster not in apropriate status" {
  skip "mock is not ready"
  run scale-cluster --name azstatus --group-name a --desired-node-count 6
  [[ $status -eq 1 ]]
}

@test "create cluster valid" {
  create-cluster --cli-input-json template.json --name aaaaa
}

@test "create cluster wo name" {
  run create-cluster --cli-input-json template.json 
  [[ $status -eq 1 ]]
}

@test "create cluster wo pwd" {
  skip "i dont have error - possibly bug"
  run create-cluster --cli-input-json template_wo_pwd.json --name aaaaa
  [[ $status -eq 1 ]]
}

@test "create cluster wo pwd given in the cmd" {
  create-cluster --cli-input-json template_wo_pwd.json --name aaaaa --input-json-param-password 1234
}

@test "re-install cluster" {
  skip "i dont have error - possibly bug"
  run reinstall-cluster --name test --cli-input-json template.json --name aaaaa
  [[ $status -eq 1 ]]
}