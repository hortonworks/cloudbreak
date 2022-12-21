#!/bin/bash

function generate_new_bcrypt_pwd() {
  local username=$1
  local pwd=$2
  local output_file=$3
  htpasswd -nbBC 10 $username $pwd > $output_file
  chmod 600 $output_file
  print_encrypted_file $output_file
}

function print_encrypted_file() {
  cat $1 | cut -d ':' -f2 | tr -d '\n'
}

function main() {
  local username=$1
  local pwd_file=$2
  local output_file=$3
  local pwd=$(cat $pwd_file | tr -d '\n')

  if ! command -v htpasswd &> /dev/null; then
    yes | yum install httpd-tools &> /dev/null
  fi

  if [[ -f "$output_file" ]]; then
    htpasswd -vb $output_file $username $pwd > /dev/null 2>&1
    local check_result="$?"
    if [[ "$check_result" != "0" ]]; then
      generate_new_bcrypt_pwd $username $pwd $output_file
    else
      print_encrypted_file $output_file
    fi
  else
      generate_new_bcrypt_pwd $username $pwd $output_file
  fi
}

main ${1+"$@"}