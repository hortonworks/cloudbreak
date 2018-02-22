# Blueprints
: ${BLUEPRINT_FILE:=blueprints/test.bp}
: ${BLUEPRINT_URL:=https://gist.githubusercontent.com/mhalmy/8309c7e4a4649fa85f38b260a38146af/raw/5c3534c7f1849ffea64a81d467d5eee801858ff7/test.bp}
: ${BLUEPRINT_NAME:="EDW-ETL: Apache Hive, Apache Spark 2"}

# Recipes
: ${RECIPE_NAME:=test-recipe}
: ${RECIPE_URL:="https://gist.githubusercontent.com/aszegedi/4fc4a6a2fd319da436df6441c04c68e1/raw/5698a1106a2365eb543e9d3c830e14f955882437/post-install.sh"}
: ${RECIPE_FILE:=scripts/recipe.sh}

# Image catalog DEV
: ${IMAGE_CATALOG_NAME:=test-catalog}
: ${IMAGE_CATALOG_NAME_DEFAULT:=cloudbreak-default}
: ${IMAGE_CATALOG_URL:="https://s3-eu-west-1.amazonaws.com/cloudbreak-info/v2-dev-cb-image-catalog.json"}

# Input JSON files
: ${AWS_INPUT_JSON_FILE:=templates/aws-template.json}
: ${OS_INPUT_JSON_FILE:=templates/kilo-openstack-template.json}

# Clusters
: ${OS_CLUSTER_NAME:=openstack-cluster}
: ${AWS_CLUSTER_NAME:=aws-cluster}

# Credentials
: ${ARM_CREDENTIAL_NAME:=azure}
: ${AWS_CREDENTIAL_NAME:=amazon}
: ${GCP_CREDENTIAL_NAME:=google}
: ${OS_CREDENTIAL_NAME:=openstack}

# Credential Arguments
: ${AWS_ARGS_KEY:="--access-key $AWS_ACCESS_KEY_ID --secret-key $AWS_SECRET_ACCESS_KEY"}
: ${AWS_ARGS_ROLE:="--role-arn $AWS_ROLE_ARN"}
: ${OS_ARGS_V2:="--tenant-user $OS_V2_USERNAME --tenant-password $OS_V2_PASSWORD --tenant-name $OS_V2_TENANT_NAME --endpoint $OS_V2_ENDPOINT --facing $OS_APIFACING"}
: ${OS_ARGS_V3:="--tenant-user $OS_V3_USERNAME --tenant-password $OS_V3_PASSWORD --keystone-scope $OS_V3_KEYSTONE_SCOPE --user-domain $OS_V3_USER_DOMAIN --endpoint $OS_V3_ENDPOINT --facing $OS_APIFACING --project-domain-name $OS_V3_PROJECT_DOMAIN --project-name $OS_V3_PROJECT_NAME"}
: ${GCP_ARGS:="--project-id $GCP_PROJECT --service-account-id $GCP_ACCOUNT_EMAIL --service-account-private-key-file $P12_PATH"}
: ${ARM_ARGS:="--subscription-id $ARM_SUBSCRIPTION_ID --tenant-id $ARM_TENANT_ID --app-id $ARM_APP_ID --app-password $ARM_PASSWORD"}
: ${COMMON_ARGS_WO_CLUSTER:="--server $BASE_URL --username $USERNAME_CLI --password $PASSWORD_CLI"}
