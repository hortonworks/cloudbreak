#!/usr/bin/env bats
load ../commands

@test "change ambari pwd" {
  change-ambari-password --name aaaaa --old-password 1234 --new-password 4321 --ambari-user admin
}

