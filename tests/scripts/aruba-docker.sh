#!/bin/bash -x

echo "CBD Version: "$TARGET_CBD_VERSION
echo "CBD URL: "$BASE_URL
echo "CBD Username: "$USERNAME_CLI
echo "CBD Password: "$PASSWORD_CLI
echo "CLI Tests: "$CLI_TEST_FILES
echo "HOME path: "$HOME

if [[ "${TARGET_CBD_VERSION}" != "MOCK" ]]; then
    echo "Get DP CLI for "$TARGET_CBD_VERSION
    wget --continue --no-check-certificate https://s3-us-west-2.amazonaws.com/cb-cli/cb-cli_"${TARGET_CBD_VERSION}"_$(uname)_x86_64.tgz -O - | tar -xvz --directory /usr/local/bin
    echo "DP CLI version is: "$(dp -v)
fi

echo "Get DP Token for CLI"
token=$(wget --continue --no-check-certificate $BASE_URL/oidc/authorize?username=$USERNAME_CLI\&tenant=hortonworks -O -)

echo "Configure DP CLI to Server: $BASE_URL User: $USERNAME_CLI"
echo $token | dp configure --server $BASE_URL --workspace $USERNAME_CLI

echo "Running RSpec with "$CLI_TEST_FILES
mkdir -p tmp/aruba
rspec -f RspecJunitFormatter -o test-result.xml -f h $CLI_TEST_FILES | tee test-result.html | ruby -n spec/common/integration_formatter.rb
export RESULT=$?

allure generate --clean allure/allure-results -o allure/allure-report

if [[ $RESULT -eq 0 ]]; then
    cat test-result.html | grep -e ".*script.*totals.*failure" | cut -d ',' -f 2 | grep -e " 0"
    export RESULT=$?
fi

chmod -Rf 777 allure

exit $RESULT