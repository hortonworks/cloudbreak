#!/usr/bin/env bats
load ../commands

@test "list ldaps" {
  list-ldaps 
}

@test "delete ldap" {
  delete-ldap --name aaaaa
}

@test "create ldap" {
  create-ldap --name ldap --ldap-server ldap://1.1.1.1:1 --ldap-domain xample.com --ldap-bind-dn "CN=a" --ldap-bind-password 123 --ldap-directory-type LDAP --ldap-user-search-base "CN=a" --ldap-user-object-class user --ldap-user-name-attribute name --ldap-group-member-attribute alma --ldap-group-name-attribute hogolyo --ldap-group-object-class 123 --ldap-group-search-base maca
}

