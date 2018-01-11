echo Hello world!

cb configure --server $BASE_URL --username $USERNAME_CLI --password $PASSWORD_CLI
bats -t e2e/*.bats > report.tap
cat report.tap
