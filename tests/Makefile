GIT_FIRST_PARENT ?= $(shell git describe --tags --first-parent | sed -nre 's/^[^0-9]*(([0-9]+\.)*[0-9]-*([a-z]+\.)*[0-9]+).*/\1/p')
GIT_LATEST_TAG ?= $(shell git describe --tags | grep -Eo "^[0-9]+\.[0-9]+\.[0-9]+\-[a-z]+\.[0-9]*")
GIT_ACTIVE_BRANCH ?= $(shell git rev-parse HEAD)
DOCKER_IP ?= $(shell echo $(DOCKER_HOST) | awk -F/ '{print $$3}' | sed 's/:.*//')
BLUEPRINT_URL ?= https://raw.githubusercontent.com/hortonworks/cb-cli/$(GIT_ACTIVE_BRANCH)/tests/blueprints/test.bp
RECIPE_URL ?= https://raw.githubusercontent.com/hortonworks/cb-cli/$(GIT_ACTIVE_BRANCH)/tests/recipes/echo.sh

ifeq ($(DOCKER_IP),)
    DOCKER_IP=127.0.0.1
endif

all: start-mock integration-test stop-mock

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

# Start a new Cloudbreak Mock with new Swagger JSON and renewed Mock IP
# For custom version apply as: 'GIT_FIRST_PARENT=2.8.0-dev.374 make start-mock'
start-mock: download-s3
	ORIGINAL_IP=127.0.0.1 NEW_IP=$(DOCKER_IP) CLEANUP=true scripts/ip-replace.sh
	GIT_VERSION=$(GIT_FIRST_PARENT) BASE_URL=https://$(DOCKER_IP) tmp/cbm.sh

# Stop Cloudbreak Mock then set back the 127.0.0.1 as default Mock IP
stop-mock:
	GIT_VERSION=$(GIT_FIRST_PARENT) docker-compose -f tmp/docker-compose.yml -p cbreak down
	ORIGINAL_IP=$(DOCKER_IP) NEW_IP=127.0.0.1 CLEANUP=false scripts/ip-replace.sh

# Restart Cloudbreak Mock (only Containers are restarted) with same Swagger JSON and Mock IP as was set previously
restart-mock:
	GIT_VERSION=$(GIT_FIRST_PARENT) docker-compose -f tmp/docker-compose.yml -p cbreak down
	GIT_VERSION=$(GIT_FIRST_PARENT) BASE_URL=https://$(DOCKER_IP) tmp/cbm.sh

# For custom version apply as: 'GIT_FIRST_PARENT=2.8.0-dev.374 make download-s3'
download-s3:
	curl -k https://s3-eu-central-1.amazonaws.com/cloudbreak-swagger/swagger-$(GIT_FIRST_PARENT).json -o swagger.json

# Start a new Cloudbreak Mock with new Swagger JSON and renewed Mock IP
#   For custom version apply like: 'GIT_FIRST_PARENT=2.8.0-dev.374 make start-mock'
# Execute an entire test suite or just one scenario you can start this with: 'CLI_TEST_FILES=spec/integration/credential.rb make integration-test'
integration-test:
	BLUEPRINT_URL=$(BLUEPRINT_URL) RECIPE_URL=$(RECIPE_URL) tmp/docker-aruba-test.sh

# Create then source your local E2E testing environment variables like 'tests/localvars'
# Execute an entire test suite or just one scenario you can start this with: 'CLI_TEST_FILES=spec/e2e/credential.rb make e2e-test'
e2e-test:
	BLUEPRINT_URL=$(BLUEPRINT_URL) RECIPE_URL=$(RECIPE_URL) scripts/docker-aruba-test.sh

clean-after-tests:
	rm -rf test_log/ tmp/ allure/ .cb test-result.html test-result.xml

.PHONY:
	all