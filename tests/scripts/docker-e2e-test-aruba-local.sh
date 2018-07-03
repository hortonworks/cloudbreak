docker pull halmy/aruba-rspec:1.0

docker run -t -d --name=aruba_test halmy/aruba-rspec:1.0 tail -f /dev/null

docker cp $WORKSPACE/tests/aruba/. aruba_test:/tmp/

docker exec -t -e BASE_URL="${BASE_URL}" \
  -e USERNAME_CLI \
  -e PASSWORD_CLI \
  -e OS_V2_ENDPOINT \
  -e OS_V2_USERNAME \
  -e OS_V2_PASSWORD \
  -e OS_V2_TENANT_NAME \
  -e TARGET_CBD_VERSION \
  -e INTEGRATIONTEST_RDSCONFIG_RDSUSER \
  -e INTEGRATIONTEST_RDSCONFIG_RDSPASSWORD \
  -e INTEGRATIONTEST_RDSCONFIG_RDSCONNECTIONURL \
  -e INTEGRATIONTEST_LDAPCONFIG_LDAPSERVERHOST \
  -e INTEGRATIONTEST_LDAPCONFIG_BINDPASSWORD \
  -e INTEGRATIONTEST_PROXYCONFIG_PROXYHOST \
  -e INTEGRATIONTEST_PROXYCONFIG_PROXYUSER \
  -e INTEGRATIONTEST_PROXYCONFIG_PROXYPASSWORD \
  -e CLI_TEST_FILES="${CLI_TEST_FILES}" \
aruba_test /tmp/scripts/test_run.sh


docker cp aruba_test:/tmp/out.html $WORKSPACE/tests/aruba/
docker cp aruba_test:/tmp/junit.xml $WORKSPACE/tests/aruba/ 

docker stop aruba_test
docker rm aruba_test