# Testing project for Cloudbreak CLI
Aruba-RSpec Test Framework for Cloudbreak CLI. The aruba test project is located at [/tests](/tests) folder.

## Local development setup
1. `make deps`
2. `source /Users/$USER/.rvm/scripts/rvm`
    > * More info about RVM install at [https://rvm.io/rvm/install](https://rvm.io/rvm/install)
    > * "To start using RVM you need to source your `.rvm/scripts/rvm` in all your open shell windows, in rare cases you need to reopen all shell windows."

### Create new test cases
Test cases use [command_builder](spec/common/command_builder.rb) and [command_helper](spec/common/command_helpers.rb) to run the CB CLI commands. So you do not 
need to add exact cli commands in test cases, for example:
```
cb.credential.describe.name(“test”).build
```

You can use the `builds` method to hide sensitive data in the generated report file:
```
cb.credential.create.openstack.keystone_v2.name(@os_credential_name).tenant_user(ENV['OS_V2_USERNAME']).
tenant_password(ENV['OS_V2_PASSWORD']).tenant_name(ENV['OS_V2_TENANT_NAME']).endpoint(ENV['OS_V2_ENDPOINT']).builds
```

### Run tests on your local machine
**Run a specific test, for example:**
```
rspec spec/integration/credential.rb
```

**Run all tests:**
```
rspec spec/integration/*.rb
```

**Run all tests with formatted HTML test reports:**
```
rspec -f RspecJunitFormatter -o test-result.xml -f h spec/integration/*.rb | tee test-result.html | ruby -n spec/common/integration_formatter.rb
```
> You can create your own script for test execution like [scripts/test_run.sh](scripts/test_run.sh).

## Run tests in Docker container
Your [Docker Machine](https://docs.docker.com/machine/reference/start/) has to be up and running before step forward.

### E2E testing
#### Preconditions 
Create your own End to End testing local environment (for example: [localvars](localvars))
> Source your E2E localvars (`. localvars` or `source localvars`)

You can find the related Make target at [CB-CLI Makefile](../Makefile)
```
make e2e-test
```
> Or you can use the test project own Make target at [Tests Makefile](Makefile)
> You can check test results at `tests/out.html`

### Integration testing
You can find the related Make target at [CB-CLI Makefile](../Makefile)
```
make integration-test
```
Or you can use the test project own Make target at [Tests Makefile](Makefile)
```
make all
```
> You can check test results at `tests/test-result.html` or `tests/test-result.xml`

#### Run a specific test scenario
```
CLI_TEST_FILES=spec/integration/credential.rb make integration-test'
```

## CB-CLI Mock
We created a [CBD Mock](https://github.com/hortonworks/cloud-swagger-mock) for CB-CLI Integration testing purposes. This is a JavaScript base Swagger mock
 for CBD.

You can use CBD mock with the help of Make targets at [Tests Makefile](Makefile).

You can start, stop or restart the CBD Mock step-by-step if you would like with the help of the following make targets: 
* **Download S3**: Download the versioned Swagger Json file from out AWS S3 bucket
    
    ```make download-s3```
* **Start mock**: Start CBD mock for CB-CLI based on the downloaded Swagger Json
    
    ```make start-mock```
* **Stop mock**: Kill all the CBD mock related containers
    
    ```make stop-mock```
* **Restart mock**: Kill all the CBD mock containers then start these again based on the existing [swagger.json](swagger.json)
    
    ```make restart-mock```