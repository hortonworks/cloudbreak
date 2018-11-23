#!/bin/bash

source /usr/share/rvm/scripts/rvm
cd /tmp

curl -Ls https://s3-us-west-2.amazonaws.com/cb-cli/cb-cli_"${TARGET_CBD_VERSION}"_$(uname)_x86_64.tgz | tar -xvz
export PATH=$(pwd):$PATH

cb configure --server $BASE_URL --username $USERNAME_CLI --password $PASSWORD_CLI

rspec -f RspecJunitFormatter -o junit.xml -f h $CLI_TEST_FILES | tee out.html | ruby -n spec/common/integration_formatter.rb