echo Hello world!

cb configure --server $BASE_URL --username $USERNAME_CLI --password $PASSWORD_CLI
bats --tap -d e2e/*.bats | tee report.tap
cat report.tap
