docker run -it \
  -v $(pwd):/work \
  -w /work \
  -e  BASE_URL \
  -e USERNAME_CLI \
  -e PASSWORD_CLI \
  -e OS_V2_ENDPOINT \
  -e OS_V2_USERNAME \
  -e OS_V2_PASSWORD \
  -e OS_V2_TENANT_NAME \
  -e OS_V3_ENDPOINT \
  -e OS_V3_USERNAME \
  -e OS_V3_PASSWORD \
  -e OS_V3_KEYSTONE_SCOPE \
  -e OS_V3_USER_DOMAIN \
  -e OS_V3_PROJECT_NAME \
  -e OS_V3_PROJECT_DOMAIN \
  -e OS_APIFACING \
  -e OS_REGION \
  afarsang/docker-bats-jq ./scripts/docker-e2e-test.sh
