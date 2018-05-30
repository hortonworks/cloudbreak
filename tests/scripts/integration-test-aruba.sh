#!/usr/bin/env bash

: ${BASE_URL:="https://127.0.0.1"}
: ${USERNAME_CLI:="admin@example.com"}
: ${PASSWORD_CLI:="cloudbreak"}

export BASE_URL=$BASE_URL
export USERNAME_CLI=$USERNAME_CLI
export PASSWORD_CLI=$PASSWORD_CLI

echo "Configure CB CLI to " $BASE_URL $USERNAME_CLI $PASSWORD_CLI
cb configure --server $BASE_URL --username $USERNAME_CLI --password $PASSWORD_CLI

echo "Path is inside Docker container: " $(pwd)
echo "Docker container Home is: " "${HOME}"

source /usr/share/rvm/scripts/rvm

cd work/aruba

rspec -f RspecJunitFormatter -o test-result.xml -f h spec/integration/*.rb | tee test-result.html | ruby -n spec/common/integration_formatter.rb