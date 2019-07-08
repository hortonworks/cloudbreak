GIT_FIRST_PARENT ?= $(shell git describe --tags --first-parent | sed -nre 's/^[^0-9]*(([0-9]+\.)*[0-9]-*([a-z]+\.)*[0-9]+).*/\1/p')
GIT_LATEST_TAG ?= $(shell git describe --tags | grep -Eo "^[0-9]+\.[0-9]+\.[0-9]+\-[a-z]+\.[0-9]*")
GIT_ACTIVE_BRANCH ?= $(shell git rev-parse HEAD)
CLUSTER_DEFINITION_URL ?= https://raw.githubusercontent.com/hortonworks/cb-cli/$(GIT_ACTIVE_BRANCH)/tests/blueprints/test.bp

all: start-mock integration-test stop-mock clean-after-tests

clean: stop-mock clean-after-tests

# After 'make deps' you should 'source /Users/$USER/.rvm/scripts/rvm'
deps:
	gpg --keyserver hkp://keys.gnupg.net --recv-keys 409B6B1796C275462A1703113804BB82D39DC0E3
	curl -sSL https://get.rvm.io | bash -s stable --ruby
	gem install rspec
	gem install aruba
	gem install aruba-rspec
	gem install json
	gem install rspec-json_expectations
	gem install rspec_junit_formatter
	gem install allure-rspec

# Start a new Mock with new Swagger JSON
# For custom version apply as: 'GIT_FIRST_PARENT=2.8.0-dev.374 make start-mock'
start-mock: download-s3
	CLEANUP=true scripts/mock-test-helper.sh
	GIT_VERSION=$(GIT_FIRST_PARENT) tmp/cbm.sh

# Stop DataPlane Mock
stop-mock:
	STOP_MOCK=true tmp/cbm.sh

# Restart DataPlane Mock (only Containers are restarted) with same Swagger JSON
restart-mock: stop-mock
	GIT_VERSION=$(GIT_FIRST_PARENT) tmp/cbm.sh

# For custom version apply as: 'GIT_FIRST_PARENT=2.8.0-dev.374 make download-s3'
download-s3:
	curl -k https://s3-eu-central-1.amazonaws.com/cloudbreak-swagger/swagger-$(GIT_FIRST_PARENT).json -o swagger-cloudbreak.json
	curl -k https://s3-us-east-2.amazonaws.com/environment-swagger/swagger-$(GIT_FIRST_PARENT).json -o swagger-environment.json
	curl -k https://s3-us-east-2.amazonaws.com/freeipa-swagger/swagger-$(GIT_FIRST_PARENT).json -o swagger-freeipa.json
	curl -k https://s3-us-east-2.amazonaws.com/redbeams-swagger/swagger-$(GIT_FIRST_PARENT).json -o swagger-redbeams.json
	curl -k https://s3-us-east-2.amazonaws.com/datalake-swagger/swagger-$(GIT_FIRST_PARENT).json -o swagger-datalake.json
	curl -k https://s3-us-east-2.amazonaws.com/autoscale-swagger/swagger-$(GIT_FIRST_PARENT).json -o swagger-autoscale.json

# You can re-run a test case or an entire test suite with no Docker Pull
rerun-integration-test:
	CLUSTER_DEFINITION_URL=$(CLUSTER_DEFINITION_URL) IMAGE_UPDATE=false tmp/docker-test.sh

# Start a new DataPlane Mock with new Swagger JSON
#   For custom version apply like: 'GIT_FIRST_PARENT=2.8.0-dev.374 make start-mock'
# Execute an entire test suite or just one scenario you can start this with: 'CLI_TEST_FILES=spec/integration/credential.rb make integration-test'
integration-test:
	CLUSTER_DEFINITION_URL=$(CLUSTER_DEFINITION_URL) tmp/docker-test.sh

# Create then source your local E2E testing environment variables like 'tests/localvars'
# Execute an entire test suite or just one scenario you can start this with: 'CLI_TEST_FILES=spec/e2e/credential.rb make e2e-test'
e2e-test:
	CLUSTER_DEFINITION_URL=$(CLUSTER_DEFINITION_URL) scripts/docker-test.sh

clean-after-tests:
	sudo chmod -Rf 777 tmp/
	rm -rf test_log/ tmp/ .dp test-result.xml swagger-*.json

.PHONY:
	all