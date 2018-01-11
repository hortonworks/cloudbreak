#!/bin/bash

# Here is an explanation for variable definitions:
#
# For example `: ${AZURE_CREDENTIAL_NAME:="azure"}`:
# sets AZURE_CREDENTIAL_NAME to "azure" if AZURE_CREDENTIAL_NAME is either unset or null.
# However `${AZURE_CREDENTIAL_NAME="azure"}` (with NO `:`) only sets the value of AZURE_CREDENTIAL_NAME if AZURE_CREDENTIAL_NAME is currently unset
# i.e., it won't change AZURE_CREDENTIAL_NAME from "" to value.
#
# The equivalent code:
# ```
# if [[ -z $AZURE_CREDENTIAL_NAME ]]
# then
#   AZURE_CREDENTIAL_NAME="azure"
# fi
# ```
#
# You can also execute a command and set the value to returned value (output). For example if the variable NOW is not already set,
# execute command date and set the variable to the today's date:
# ```
# : ${NOW:=$(date +"%m-%d-%Y")}
# ```

# Blueprints
: ${BLUEPRINT_FILE:="blueprints/test.bp"}
: ${BLUEPRINT_URL:=https://gist.githubusercontent.com/mhalmy/8309c7e4a4649fa85f38b260a38146af/raw/5c3534c7f1849ffea64a81d467d5eee801858ff7/test.bp}
: ${BLUEPRINT_NAME:="EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin 0.7.0"}

# Recipes
: ${RECIPE_NAME:="test-recipe"}
: ${RECIPE_URL:="https://gist.githubusercontent.com/aszegedi/4fc4a6a2fd319da436df6441c04c68e1/raw/5698a1106a2365eb543e9d3c830e14f955882437/post-install.sh"}
: ${RECIPE_FILE:="scripts/recipe.sh"}

# Image catalog
: ${IMAGE_CATALOG_NAME:="test-catalog"}
: ${IMAGE_CATALOG_NAME_DEFAULT:="cloudbreak-default"}
: ${IMAGE_CATALOG_URL:="https://s3-eu-west-1.amazonaws.com/cloudbreak-info/v2-dev-cb-image-catalog.json"}

# Input JSON files
: ${AWS_INPUT_JSON_FILE:="templates/aws-template.json"}
: ${OPENSTACK_INPUT_JSON_FILE:="templates/openstack-template.json"}

# Clusters
: ${OPENSTACK_CLUSTER_NAME:="openstack-cluster"}
: ${AWS_CLUSTER_NAME:="aws-cluster"}

# Credentials
: ${AZURE_CREDENTIAL_NAME:="azure"}
: ${AWS_CREDENTIAL_NAME:="amazon"}
: ${GCP_CREDENTIAL_NAME:="google"}
: ${OS_CREDENTIAL_NAME:="openstack"}
: ${TEST_CREDENTIAL_NAME:="test-cred"}

# OpenStack
: ${OS_V2_ENDPOINT:=http://openstack.eng.com:3000/v2.0}
: ${OS_V3_ENDPOINT:=http://123.45.678.90:5000/v3}
: ${OS_USERNAME:="cloudbreak"}
: ${OS_PASSWORD:="cloudbreak"}
: ${OS_TENANT_NAME:="cloudbreak"}
: ${OS_USER_DOMAIN:="Default"}
: ${OS_APIFACING:="Internal"}
: ${OS_NETWORK:="PROVIDER_NET_172.22.64.0/18"}
: ${OS_SUBNET:="PROVIDER_SUBNET_172.22.64.0/18"}
: ${OS_MASTER_SECURITY_GROUP:="aszegedi-master"}
: ${OS_WORKER_SECURITY_GROUP:="aszegedi-worker"}
: ${OS_COMPUTE_SECURITY_GROUP:="aszegedi-compute"}

# Azure
: ${AZURE_SUBSCRIPTION_ID:="a12b1234-1234-12aa-3bcc-4d5e6f78900g"}
: ${AZURE_TENANT_ID:="a12b1234-1234-12aa-3bcc-4d5e6f78900g"}
: ${AZURE_APP_ID:="a12b1234-1234-12aa-3bcc-4d5e6f78900g"}
: ${AZURE_PASSWORD:="password123"}

# AWS
: ${AWS_ROLE_ARN:="arn:aws:iam::1234567890:role/auto-test"}
: ${AWS_ACCESS_KEY_ID:="ABCDEFGHIJKLMNO123BC"}
: ${AWS_SECRET_ACCESS_KEY:="+ABcdEFGHIjKLmNo+ABc0dEF1G23HIjKLmNopqrs"}
: ${AWS_REGION:="eu-west-1"}

# GCP
: ${GCP_PROJECT_ID:="cloudbreak"}
: ${GCP_ACCOUNT_EMAIL:="1234567890-abcde1fghijk2lmn1o2p34q5r7stuvz@developer.gserviceaccount.com"}
: ${P12_PATH:=keys/test.p12}

# Credential Arguments
AWS_ARGS_KEY=" --access-key ${AWS_ACCESS_KEY_ID} --secret-key ${AWS_SECRET_ACCESS_KEY}"
AWS_ARGS_ROLE=" --role-arn  ${AWS_ROLE_ARN}"
OPENSTACK_ARGS_V2=" --name ${OS_CREDENTIAL_NAME} --tenant-user ${OS_USERNAME} --tenant-password ${OS_PASSWORD} --tenant-name ${OS_TENANT_NAME} --endpoint ${OS_V2_ENDPOINT}"
OPENSTACK_ARGS_V3=" --name ${OS_CREDENTIAL_NAME} --tenant-user ${OS_USERNAME} --tenant-password ${OS_PASSWORD} --user-domain ${OS_USER_DOMAIN} --endpoint ${OS_V3_ENDPOINT}"
GCP_ARGS=" --name ${GCP_CREDENTIAL_NAME} --project-id ${GCP_PROJECT_ID} --service-account-id ${GCP_ACCOUNT_EMAIL} --service-account-private-key-file ${P12_PATH}"
AZURE_ARGS=" --name ${AZURE_CREDENTIAL_NAME} --subscription-id ${AZURE_SUBSCRIPTION_ID} --tenant-id ${AZURE_TENANT_ID} --app-id ${AZURE_APP_ID} --app-password ${AZURE_PASSWORD}"

COMMON_ARGS_WO_CLUSTER=" --server ${BASE_URL} --username ${USERNAME_CLI} --password ${PASSWORD_CLI}  "
