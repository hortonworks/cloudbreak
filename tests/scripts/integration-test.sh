#!/usr/bin/env bash

export BASE_URL=https://127.0.0.1
export USERNAME_CLI=admin@example.com
export PASSWORD_CLI=cloudbreak
#could be a bug:
cb configure --server $BASE_URL --username $USERNAME_CLI --password $PASSWORD_CLI

bats --tap integration/*.bats | tee test-result.tap
