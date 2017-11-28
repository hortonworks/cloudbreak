#!/usr/bin/env bats
load commands

@test "generate template azure existing" {
  generate-cluster-template azure existing-subnet| jq '. "network" | [to_entries[].key] == ["subnetCIDR"]'
}

@test "generate template azure new" {
  generate-cluster-template azure new-network| jq '. "network" | [to_entries[].key] == ["parameters"]'
}

@test "generate template aws existing netw" {
  generate-cluster-template aws existing-network | jq '. "network" | [to_entries[].key] == ["parameters","subnetCIDR"]'
}

@test "generate template aws existing subnet" {
  generate-cluster-template aws existing-subnet | jq '. "network" | [to_entries[].key] == ["parameters"]'
}

@test "generate template aws new" {
  generate-cluster-template aws new-network| jq '. "network" | [to_entries[].key] == ["subnetCIDR"]'
}

@test "Check generate cluster template openstack new network" {
   CHECK_RESULT=$( generate-cluster-template openstack new-network )
   [ $(echo $CHECK_RESULT | jq ' ."network"| ."parameters" | [to_entries[].key] == ["publicNetId"]') == true ] &&
   [ $(echo $CHECK_RESULT | jq '."network" | ."subnetCIDR" != ""' ) == true ]
}

@test "Check generate cluster template openstack existing network" {
    CHECK_RESULT=$( generate-cluster-template openstack existing-network )
   [ $(echo $CHECK_RESULT | jq ' ."network"| ."parameters" | [to_entries[].key] == ["networkId","publicNetId","routerId" ]' ) == true ] &&
   [ $(echo $CHECK_RESULT | jq '."network" | ."subnetCIDR" != ""' ) == true ]
}

@test "Check generate cluster template openstack existing subnet" {
    CHECK_RESULT=$( generate-cluster-template openstack existing-subnet )
   [ $(echo $CHECK_RESULT | jq ' ."network"| ."parameters" | [to_entries[].key] == ["networkId","networkingOption","publicNetId","subnetId"]') == true ]
}

@test "Check generate cluster template gcp new network" {
   CHECK_RESULT=$( generate-cluster-template gcp new-network )
   [ $(echo $CHECK_RESULT | jq '."network"| [to_entries[].key] == ["subnetCIDR"]') == true ] &&
   [ $(echo $CHECK_RESULT | jq '."network" | ."subnetCIDR" != ""' ) == true ]
}

@test "Check generate cluster template gcp existing network" {
   CHECK_RESULT=$( generate-cluster-template gcp existing-network )
   [ $(echo $CHECK_RESULT | jq ' ."network"| ."parameters" | [to_entries[].key] == ["networkId"]') == true ] &&
   [ $(echo $CHECK_RESULT | jq '."network" | ."subnetCIDR" != ""' ) == true ]
}

@test "Check generate cluster template gcp existing subnet" {
   CHECK_RESULT=$( generate-cluster-template gcp existing-subnet )
   [ $(echo $CHECK_RESULT | jq ' ."network"| ."parameters" | [to_entries[].key] == ["networkId","noFirewallRules","noPublicIp","subnetId"]') == true ]
}

@test "Generate reinstall template" {
  generate-reinstall-template --blueprint-name test.bp | jq '."blueprintName" == ["test.bp"]'
}