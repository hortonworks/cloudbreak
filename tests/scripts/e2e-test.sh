echo Hello world!

export CLOUD_URL=$BASE_URL
export EMAIL=$USERNAME

cb configure --server $CLOUD_URL --username $EMAIL --password $PASSWORD
bats -t e2e.bats > report.tap
cat report.tap
