GIT_FIRST_PARENT ?= $(shell git describe --tags --first-parent | sed -nre 's/^[^0-9]*(([0-9]+\.)*[0-9]-*([a-z]+\.)*[0-9]+).*/\1/p')
GIT_LATEST_TAG ?= $(shell git describe --tags | grep -Eo "^[0-9]+\.[0-9]+\.[0-9]+\-[a-z]+\.[0-9]*")
DOCKER_IP ?= $(shell echo $(DOCKER_HOST) | awk -F/ '{print $$3}' | sed 's/:.*//')

ifeq ($(DOCKER_IP),)
    DOCKER_IP=127.0.0.1
endif

define run_test
    @echo ::: ${1} E2E tests have been launched :::
    bats --tap e2e/${1}.bats | tee ${1}-result.tap
endef

all: start-mock docker-integration-test-aruba stop-mock

clean: stop-mock clean-test-result

start-mock: download-s3
	ORIGINAL_IP=127.0.0.1 NEW_IP=$(DOCKER_IP) make replace-ip
	GIT_VERSION=$(GIT_FIRST_PARENT) GIT_TAG=$(GIT_LATEST_TAG) BASE_URL=https://$(DOCKER_IP) ./scripts/cbm.sh

stop-mock:
	GIT_VERSION=$(GIT_FIRST_PARENT) docker-compose -f docker-compose.yml -p cbreak down
	ORIGINAL_IP=$(DOCKER_IP) NEW_IP=127.0.0.1 make replace-ip

integration-test:
	BASE_URL=https://$(DOCKER_IP) ./scripts/integration-test.sh

e2e-test:
	./scripts/e2e-test.sh

docker-integration-test:
	./scripts/docker-integration-test.sh

docker-e2e-test:
	./scripts/run-e2e-test.sh

docker-e2e-test-local:
	./scripts/docker-e2e-test-aruba-local.sh

docker-e2e-test-jenkins:
	./scripts/jenkins-docker-e2e-test-aruba.sh	

run-e2e-suite:
	$(call run_test,${test})

run-e2e-regression:
	bats --tap e2e/*.bats | tee regressiontest-results.tap

generate-xunit-result:
	./scripts/generate-xunit-result.sh

clean-test-result:
	rm -f test-result.tap

download-s3:
	curl -k https://s3-eu-central-1.amazonaws.com/cloudbreak-swagger/swagger-$(GIT_FIRST_PARENT).json -o swagger.json

docker-integration-test-aruba:
	./scripts/docker-integration-test-aruba.sh

replace-ip:
ifneq ($(ORIGINAL_IP), $(NEW_IP))
	find . -type f -exec sed -i 's/$(ORIGINAL_IP)/$(NEW_IP)/g' {} +
endif

.PHONY:
	all