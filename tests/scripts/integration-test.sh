#!/usr/bin/env bash

export BASE_URL=https://127.0.0.1
export USERNAME_CLI=admin@example.com
export PASSWORD_CLI=cloudbreak

echo "Configure CB CLI to " $BASE_URL $USERNAME_CLI $PASSWORD_CLI
cb configure --server $BASE_URL --username $USERNAME_CLI --password $PASSWORD_CLI

echo "Path is inside Docker container: " $(pwd)
echo "Docker container Home is: " "${HOME}"

echo "Launch Bats tests from " /work/integration/ " container folder"
cd work
bats --tap integration/*.bats | tee test-result.tap