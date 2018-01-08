echo Hello world!

export CLOUD_URL=$BASE_URL
export EMAIL=$USERNAME_CLI

cb configure --server $CLOUD_URL --username $EMAIL --password $PASSWORD_CLI
bats -t e2e/*.bats > report.tap
cat report.tap
