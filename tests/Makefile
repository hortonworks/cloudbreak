GIT_FIRST_PARENT ?= $(shell git describe --tags --first-parent | sed -nre 's/^[^0-9]*(([0-9]+\.)*[0-9]-*([a-z]+\.)*[0-9]+).*/\1/p')
GIT_LATEST_TAG ?= $(shell git describe --tags | grep -Eo "^[0-9]+\.[0-9]+\.[0-9]+\-[a-z]+\.[0-9]*")
GIT_BRANCH ?= $(shell git rev-parse --abbrev-ref HEAD)
DOCKER_IP ?= $(shell echo $(DOCKER_HOST) | awk -F/ '{print $$3}' | sed 's/:.*//')
BLUEPRINT_URL ?= https://rawgit.com/hortonworks/cb-cli/$(GIT_BRANCH)/tests/blueprints/test.bp
RECIPE_URL ?= https://rawgit.com/hortonworks/cb-cli/$(GIT_BRANCH)/tests/recipes/echo.sh

ifeq ($(DOCKER_IP),)
    DOCKER_IP=127.0.0.1
endif

define run_test
    @echo ::: ${1} E2E tests have been launched :::
    bats --tap e2e/${1}.bats | tee ${1}-result.tap
endef

all: start-mock docker-test stop-mock

clean: stop-mock clean-test-result

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
	ORIGINAL_IP=127.0.0.1 NEW_IP=$(DOCKER_IP) make replace-ip
	GIT_VERSION=$(GIT_FIRST_PARENT) GIT_TAG=$(GIT_LATEST_TAG) BASE_URL=https://$(DOCKER_IP) ./scripts/cbm.sh

# Stop Cloudbreak Mock then set back the 127.0.0.1 as default Mock IP
stop-mock:
	GIT_VERSION=$(GIT_FIRST_PARENT) docker-compose -f docker-compose.yml -p cbreak down
	ORIGINAL_IP=$(DOCKER_IP) NEW_IP=127.0.0.1 make replace-ip

# Restart Cloudbreak Mock (only Containers are restarted) with same Swagger JSON and Mock IP as was set previously
restart-mock:
	GIT_VERSION=$(GIT_FIRST_PARENT) docker-compose -f docker-compose.yml -p cbreak down
	GIT_VERSION=$(GIT_FIRST_PARENT) GIT_TAG=$(GIT_LATEST_TAG) BASE_URL=https://$(DOCKER_IP) ./scripts/cbm.sh

# For custom version apply as: 'GIT_FIRST_PARENT=2.8.0-dev.374 make download-s3'
download-s3:
	curl -k https://s3-eu-central-1.amazonaws.com/cloudbreak-swagger/swagger-$(GIT_FIRST_PARENT).json -o swagger.json

docker-test:
	BLUEPRINT_URL=$(BLUEPRINT_URL) RECIPE_URL=$(RECIPE_URL) DOCKER_TAG=$(GIT_FIRST_PARENT) ./scripts/docker-test.sh

clean-test-result:
	rm -rf test-result.html test-result.xml ruby-uuid allure .cb tmp test_log

# Replace the default Mock IP to the locally found Docker IP
replace-ip:
ifneq ($(ORIGINAL_IP), $(NEW_IP))
	find . -not -path "./Makefile" -type f -exec sed -i 's/$(ORIGINAL_IP)/$(NEW_IP)/g' {} +
endif

.PHONY:
	all