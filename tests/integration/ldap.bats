#!/usr/bin/env bats

load ../utils/commands

@test "Check ldaps are listed" {
  for OUTPUT in $(list-ldaps | jq ' .[] | [to_entries[].key] == ["Name","Server","Domain","BindDn","DirectoryType","UserSearchBase","UserNameAttribute","UserObjectClass","GroupMemberAttribute","GroupNameAttribute","GroupObjectClass","GroupSearchBase,"adminGroup"]');
  do
    echo $OUTPUT
    [[ "$OUTPUT" == "true" ]]
  done
 }

@test "Create ldap" {
  OUTPUT=$(create-ldap --name ldap --ldap-server ldap://1.1.1.1:1 --ldap-domain xample.com --ldap-bind-dn "CN=a" --ldap-bind-password 123 --ldap-directory-type LDAP --ldap-user-search-base "CN=a" --ldap-user-object-class user --ldap-user-name-attribute name --ldap-group-member-attribute alma --ldap-group-name-attribute hogolyo --ldap-group-object-class 123 --ldap-group-search-base maca 2>&1 | awk '{printf "%s",$0} END {print ""}' | awk 'match($0, /{[^{}]+}/) { print substr($0, RSTART, RLENGTH)}')

  [[ $(echo "${OUTPUT}" | jq ' . |  [to_entries[].key] == ["bindDn","bindPassword","directoryType","domain","groupMemberAttribute","groupNameAttribute","groupObjectClass","groupSearchBase","name","protocol","serverHost","serverPort","userNameAttribute","userObjectClass","userSearchBase"]') == true ]]
}

@test "Create ldap - invalid server without protocol" {
  OUTPUT=$(create-ldap --name ldap --ldap-server 1.1.1.1:1 --ldap-domain xample.com --ldap-bind-dn "CN=a" --ldap-bind-password 123 --ldap-directory-type LDAP --ldap-user-search-base "CN=a" --ldap-user-object-class user --ldap-user-name-attribute name --ldap-group-member-attribute alma --ldap-group-name-attribute hogolyo --ldap-group-object-class 123 --ldap-group-search-base maca 2>&1 | sed -e '$d' | sed -e '$!d')
  [[ "${OUTPUT}" == *"Invalid ldap server address format"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check ldap delete" {
  OUTPUT=$(delete-ldap  --name testldap 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"ldap config deleted"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}