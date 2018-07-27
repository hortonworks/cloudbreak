# Aruba-RSpec Test Framework for Cloudbreak CLI

The aruba test project is located at `tests/aruba/` sub-folder.

```
cd ${YOUR_PATH}/cb-cli/tests/aruba
```
>  `YOUR_PATH` where your cb-cli project has been cloned.

## E2E testing

### Install and run everything on your local machine:

1. `curl -sSL https://get.rvm.io | bash -s stable --ruby`
2. `source /Users/$USER/.rvm/scripts/rvm` (More info about RVM install: https://rvm.io/rvm/install)
3. `gem install rspec`
4. `gem install aruba`
5. `gem install aruba-rspec`
6. `gem install json`
7. `gem install rspec-json_expectations`
8. `gem install rspec_junit_formatter`
9. Edit localvars (you can find an example at [localvars](localvars))
10. Source localvars (`. localvars` or `source localvars`)

**Run a specific test:**
```
rspec spec/e2e/cluster.rb
```

**Run all the tests:**
```
rspec spec/e2e/*.rb
```

**Run with all the tests with generated test reports:**
```
rspec -f RspecJunitFormatter -o test-result.xml -f h spec/e2e/*.rb | tee test-result.html | ruby -n spec/common/integration_formatter.rb
```

### Run with Docker:

**Preconditions:**
1. Your Docker Machine has to be up and running
2. Edit localvars (you can find an example at [localvars](localvars))
3. Source localvars (`. localvars` or `source localvars`)

**Run tests:**
```
make -C ../ docker-aruba-test
```
> You can check test results at `tests/aruba/out.html`

## Integration testing

Go to the `cb-cli` root folder (where the [Makefile](../../Makefile) is located) then:
```
make integration-test
```
> You can check test results at `tests/aruba/test-result.html` or `tests/aruba/test-result.xml`

## Create/Add new test cases in ruby - for test developers

The following section describes test cases and gives some guidelines on how to write new ones in ruby.

Test cases use a `commandbuilder` to run the CB CLI commands. So here are some ruby code examples:
```
cb.credential.describe.name(“test”).build
```

or just use the `builds` method to hide sensitive data in the generated report file:
```
cb.credential.create.openstack.keystone_v2.name(@os_credential_name).tenant_user(ENV['OS_V2_USERNAME']).
tenant_password(ENV['OS_V2_PASSWORD']).tenant_name(ENV['OS_V2_TENANT_NAME']).endpoint(ENV['OS_V2_ENDPOINT']).builds
```