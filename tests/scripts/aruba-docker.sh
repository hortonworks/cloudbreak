#!/bin/bash -x

echo "CBD Version: "$TARGET_CBD_VERSION
echo "CBD URL: "$BASE_URL
echo "CBD Username: "$USERNAME_CLI
echo "CBD Password: "$PASSWORD_CLI
echo "CLI Tests: "$CLI_TEST_FILES
echo "HOME path: "$HOME

if [[ "${TARGET_CBD_VERSION}" != "MOCK" ]]; then
    echo "Get CB CLI for "$TARGET_CBD_VERSION
    curl --verbose --show-error --location --insecure -C - https://s3-us-west-2.amazonaws.com/cb-cli/cb-cli_"${TARGET_CBD_VERSION}"_$(uname)_x86_64.tgz | tar -xvz --directory /usr/local/bin
    if [[ $? -ne 0 ]]; then
        wget --continue --no-check-certificate https://s3-us-west-2.amazonaws.com/cb-cli/cb-cli_"${TARGET_CBD_VERSION}"_$(uname)_x86_64.tgz -O - | tar -xvz --directory /usr/local/bin
    fi
    echo "CB CLI version is: "$(cb -v)
fi

echo "Configure CB CLI to Server: "$BASE_URL" User: "$USERNAME_CLI" Password: "$PASSWORD_CLI
cb configure --server $BASE_URL --username $USERNAME_CLI --password $PASSWORD_CLI --workspace $USERNAME_CLI 

echo "Running RSpec with "$CLI_TEST_FILES
rspec -f RspecJunitFormatter -o test-result.xml -f h $CLI_TEST_FILES | tee test-result.html | ruby -n spec/common/integration_formatter.rb
export RESULT=$?

allure generate --clean allure/allure-results -o allure/allure-report

if [[ $RESULT -eq 0 ]]; then
    cat test-result.html | grep -e ".*script.*totals.*failure" | cut -d ',' -f 2 | grep -e " 0"
    export RESULT=$?
fi

chmod -Rf 777 allure

exit $RESULT