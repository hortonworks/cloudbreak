#!/usr/bin/env bash
set -x

: ${BASE_URL?"Need to set BASE_URL"}
: ${USERNAME_CLI?"Need to set USERNAME_CLI"}
: ${PASSWORD_CLI?"Need to set PASSWORD_CLI"}

# OpenStack
: ${OS_V2_ENDPOINT?"Need to set OS_V2_ENDPOINT"}
: ${OS_V2_USERNAME?"Need to set OS_V2_USERNAME"}
: ${OS_V2_PASSWORD?"Need to set OS_V2_PASSWORD"}
: ${OS_V2_TENANT_NAME?"Need to set OS_V2_TENANT_NAME"}
: ${OS_V3_ENDPOINT?"Need to set OS_V3_ENPOINT"}
: ${OS_V3_USERNAME?"Need to set OS_V3_USERNAME"}
: ${OS_V3_PASSWORD?"Need to set OS_V3_PASSWORD"}
: ${OS_V3_KEYSTONE_SCOPE?"Need to set OS_V3_KEYSTONE_SCOPE"}
: ${OS_V3_USER_DOMAIN?"Need to set OS_V3_USER_DOMAIN"}
: ${OS_V3_PROJECT_NAME?"Need to set OS_V3_PROJECT_NAME"}
: ${OS_V3_PROJECT_DOMAIN?"Need to set OS_V3_PROJECT_DOMAIN"}
: ${OS_APIFACING?"Need to set OS_APIFACING"}
: ${OS_REGION?"Need to set OS_REGION"}

# Get CBD Version
if [[ -z "$(echo $TARGET_CBD_VERSION)" ]]; then
	export TARGET_CBD_VERSION=$(curl -sk $BASE_URL/cb/info | grep -oP "(?<=\"version\":\")[^\"]*")
	echo "CBD version: "$TARGET_CBD_VERSION
fi
echo TARGET_CBD_VERSION

# Get CB CLI to Jenkins machine
curl -Ls https://s3-us-west-2.amazonaws.com/cb-cli/cb-cli_"${TARGET_CBD_VERSION}"_$(uname)_x86_64.tgz | tar -xvz --directory /usr/local/bin

cb configure --server $BASE_URL --username $USERNAME_CLI --password $PASSWORD_CLI
bats --tap e2e/*.bats | tee report.tap
