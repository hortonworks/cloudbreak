#!/usr/bin/env bats

load ../utils/e2e_parameters
load ../utils/commands

@test "Check generate cluster template azure new network" {
  OUTPUT=$(generate-cluster-template azure new-network | jq '. "network" | [to_entries[].key] == ["subnetCIDR"]')

  [[ "${OUTPUT}" == true ]]
}

@test "Check generate cluster template azure existing subnet" {
  OUTPUT=$(generate-cluster-template azure existing-subnet | jq ' ."network"| ."parameters" | [to_entries[].key] == ["networkId","noFirewallRules","noPublicIp","resourceGroupName","subnetId"]')

  [[ "${OUTPUT}" == true ]]
}

@test "Check generate cluster template aws new network" {
  OUTPUT=$(generate-cluster-template aws new-network | jq '. "network" | [to_entries[].key] == ["subnetCIDR"]')

  [[ "${OUTPUT}" == true ]]
}

@test "Check generate cluster template aws existing network" {
  OUTPUT=$(generate-cluster-template aws existing-network | jq ' ."network"| ."parameters" | [to_entries[].key] == ["internetGatewayId","vpcId"]')

  [[ "${OUTPUT}" == true ]]
}
@test "Check generate cluster template aws existing subnet" {
  OUTPUT=$(generate-cluster-template aws existing-subnet | jq ' ."network"| ."parameters" | [to_entries[].key] == ["subnetId","vpcId"]')

  [[ "${OUTPUT}" == true ]]
}

@test "Check generate cluster template openstack new network" {
  OUTPUT=$(generate-cluster-template openstack new-network)
  [ $(echo $OUTPUT | jq ' ."network"| ."parameters" | [to_entries[].key] == ["publicNetId"]') == true ] &&
  [ $(echo $OUTPUT | jq '."network" | ."subnetCIDR" != ""' ) == true ]
}

@test "Check generate cluster template openstack existing network" {
  OUTPUT=$(generate-cluster-template openstack existing-network)
  [ $(echo $OUTPUT | jq ' ."network"| ."parameters" | [to_entries[].key] == ["networkId","publicNetId","routerId" ]' ) == true ] &&
  [ $(echo $OUTPUT | jq '."network" | ."subnetCIDR" != ""' ) == true ]
}

@test "Check generate cluster template openstack existing subnet" {
  OUTPUT=$(generate-cluster-template openstack existing-subnet | jq ' ."network"| ."parameters" | [to_entries[].key] == ["networkId","networkingOption","publicNetId","subnetId"]')

  [[ "${OUTPUT}" == true ]]
}

@test "Check generate cluster template gcp new network" {
  OUTPUT=$(generate-cluster-template gcp new-network)
  [ $(echo $OUTPUT | jq '."network"| [to_entries[].key] == ["subnetCIDR"]') == true ] &&
  [ $(echo $OUTPUT | jq '."network" | ."subnetCIDR" != ""' ) == true ]
}

@test "Check generate cluster template gcp existing network" {
  OUTPUT=$(generate-cluster-template gcp existing-network)
  [ $(echo $OUTPUT | jq ' ."network"| ."parameters" | [to_entries[].key] == ["networkId"]') == true ] &&
  [ $(echo $OUTPUT | jq '."network" | ."subnetCIDR" != ""' ) == true ]
}

@test "Check generate cluster template gcp existing subnet" {
  OUTPUT=$(generate-cluster-template gcp existing-subnet | jq ' ."network"| ."parameters" | [to_entries[].key] == ["networkId","noFirewallRules","noPublicIp","subnetId"]')

  [[ "${OUTPUT}" == true ]]
}
