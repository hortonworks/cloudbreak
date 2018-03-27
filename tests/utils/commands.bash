#!/usr/bin/env bash

function cb-command() {
  DEBUG=1 cb "$@"
}

function configure-cb() {
  cb-command configure "$@"
}

function generate-cluster-template() {
  cb-command cluster generate-template "$@"
}

function list-blueprints() {
  cb-command blueprint list
}

 function create-blueprint {
  cb-command blueprint create "$@"
}

 function delete-blueprint() {
  cb-command blueprint delete "$@"
}

 function describe-blueprint() {
  cb-command blueprint describe "$@"
}

 function create-credential-aws-key() {
  cb-command credential create aws key-based "$@"
}

 function create-credential-aws-role() {
  cb-command credential create aws role-based "$@"
}

function create-credential-openstack-v2() {
  cb-command credential create openstack keystone-v2 "$@"
}

function create-credential-openstack-v3() {
  cb-command credential create openstack keystone-v3 "$@"
}

function create-credential-azure() {
  cb-command credential create azure app-based "$@"
}

function create-credential-gcp() {
  cb-command credential create gcp "$@"
}

function list-credentials() {
  cb-command credential list
}

function describe-credential() {
  cb-command credential describe "$@"
}

function delete-credential() {
  cb-command credential delete --name "$@"
}

function create-recipe(){
  cb-command recipe create "$@"
}

function list-recipes() {
  cb-command recipe list
}

function delete-recipe() {
  cb-command recipe delete "$@"
}

function describe-recipe() {
  cb-command recipe describe "$@"
}

function create-cluster() {
  cb-command cluster create "$@"
}

function start-cluster() {
  cb-command cluster start "$@"
}

function stop-cluster() {
  cb-command cluster stop "$@"
}

function list-clusters() {
  cb-command cluster list
}

function delete-cluster() {
  cb-command cluster delete --name "$@"
}

function describe-cluster() {
  cb-command cluster describe "$@"
}

function scale-cluster() {
  cb-command cluster scale "$@"
}

function repair-cluster() {
  cb-command cluster repair "$@"
}

function sync-cluster() {
  cb-command cluster sync "$@"
}

function change-ambari-password() {
  cb-command cluster change-ambari-password "$@"
}

function reinstall-cluster() {
  cb-command cluster reinstall "$@"
}

function generate-reinstall-template() {
  cb-command cluster generate-reinstall-template "$@"
}

function list-ldaps() {
  cb-command ldap list "$@"
}

function create-ldap() {
  cb-command ldap create "$@"
}

function delete-ldap() {
  cb-command ldap delete "$@"
}

function availability-zone-list() {
  cb-command cloud availability-zones "$@"
}

function region-list() {
  cb-command cloud regions "$@"
}

function instance-list() {
  cb-command cloud instances "$@"
}

function volume-list() {
  cb-command cloud volumes "$@"
}

function list-image-catalog() {
  cb-command imagecatalog list "$@"
}

function create-image-catalog() {
  cb-command imagecatalog create "$@"
}

function get-images() {
  cb-command imagecatalog images "$@"
}

function delete-image-catalog() {
  cb-command imagecatalog delete "$@"
}

function set-default-image-catalog() {
  cb-command imagecatalog set-default "$@"
}

function list-database() {
  cb-command database list "$@"
}

function create-database() {
  cb-command database create "$@"
}

function delete-database() {
  cb-command database delete "$@"
}

function list-proxy() {
  cb-command proxy list "$@"
}

function create-proxy() {
  cb-command proxy create "$@"
}

function delete-proxy() {
  cb-command proxy delete "$@"
}