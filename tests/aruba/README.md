# Aruba-rspec testframework for CB CLI
The aruba test project is located in tests/aruba/  
 * ${YOUR_PATH} - where the cb-cli project is located  
 * ${YOUR_IP} - your docker machine ip

```
cd ${YOUR_PATH}/cb-cli/tests/aruba
```

## E2E tests

Run without Docker:
```
curl -sSL https://get.rvm.io | bash -s stable --ruby
source /Users/$USER/.rvm/scripts/rvm # More info about RVM install: https://rvm.io/rvm/install
gem install rspec
gem install aruba
gem install aruba-rspec
gem install json
gem install rspec-json_expectations
gem install rspec_junit_formatter
#Edit localvars - an example of localvars file is can be found in the test folder in ${YOUR_PATH}/tests/aruba
source localvars

rspec spec/e2e/cluster.rb  # Run a specific test
rspec spec/e2e/*.rb # Run all tests
rspec -f RspecJunitFormatter -o test-result.xml -f h spec/e2e/*.rb | tee test-result.html | ruby -n spec/common/integration_formatter.rb # Run with output files
```
Run with Docker locally:
```
#Start your Docker machine
docker-machine start

#Edit localvars - an example of localvars file is can be found in the test folder in ${YOUR_PATH}/cb-cli/tests/aruba
source localvars
#Run the script: 
make -C ../ docker-e2e-test-local
```
Check result in:
```
out.html
```
## Integration test
Build the CB 
```
cd ../../
make build-docker
# Then change the dir back
cd tests/aruba
```
In ../Makefile change the IP address (marked as $(YOUR_IP))
```
start-mock: download-s3
	ORIGINAL_IP=$(YOUR_IP) NEW_IP=$(DOCKER_IP) make replace-ip
	GIT_VERSION=$(GIT_FIRST_PARENT) GIT_TAG=$(GIT_LATEST_TAG) BASE_URL=https://$(DOCKER_IP) ./scripts/cbm.sh

stop-mock:
	GIT_VERSION=$(GIT_FIRST_PARENT) docker-compose -f docker-compose.yml -p cbreak down
	ORIGINAL_IP=$(DOCKER_IP) NEW_IP=$(YOUR_IP) make replace-ip
```
Change the path of CB executable in ../scripts/docker-integration-test-aruba.sh
```
 -v "${YOUR_PATH}/cb-cli/build/Linux}":/usr/local/bin \
```
Edit base url in ../scripts/integration-test-aruba.sh
```
: ${BASE_URL:="https://192.168.99.100"}
```

Run test cases:
```
make -C ../ all
```
Check result in:
```
test-result.html
```

## Create/Add new test cases in ruby - for test developers
```
The following section describes test cases and gives some guidelines on how to write new ones in ruby.

Test cases use a commandbuilder to run the CB CLI commands.

Some ruby code parts as examples:

```
cb.credential.describe.name(“test”).build
```
Or use the builds method to hide sensitive data in the generated report file.
```
cb.credential.create.openstack.keystone_v2.name(@os_credential_name).tenant_user(ENV['OS_V2_USERNAME']).
tenant_password(ENV['OS_V2_PASSWORD']).tenant_name(ENV['OS_V2_TENANT_NAME']).endpoint(ENV['OS_V2_ENDPOINT']).builds
```