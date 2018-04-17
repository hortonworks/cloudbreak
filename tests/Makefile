GIT_VERSION ?= $(shell git describe --tags --first-parent | sed -nre 's/^[^0-9]*(([0-9]+\.)*[0-9]-*([a-z]+\.)*[0-9]+).*/\1/p')
GIT_TAG ?= $(shell git describe --tags | grep -Eo "^[0-9]+\.[0-9]+\.[0-9]+\-[a-z]+\.[0-9]*")

define run_test
    @echo ::: ${1} E2E tests have been launched :::
    bats --tap e2e/${1}.bats | tee ${1}-result.tap
endef

all: cbm integration-test

cbm: 
	GIT_VERSION=$(GIT_VERSION) GIT_TAG=$(GIT_TAG) ./scripts/cbm.sh

integration-test:
	./scripts/integration-test.sh

e2e-test:
	./scripts/e2e-test.sh

run-e2e-suite:
	$(call run_test,${test})

run-e2e-regression:
	bats --tap e2e/*.bats | tee regressiontest-results.tap