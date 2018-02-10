define run_test
    @echo ::: ${1} E2E tests have been launched :::
    bats --tap e2e/${1}.bats | tee ${1}-result.tap
endef

all: cbm integration-test

cbm: 
	./scripts/cbm.sh

integration-test:
	./scripts/integration-test.sh

e2e-test:
	./scripts/e2e-test.sh

run-e2e-suite:
	$(call run_test,${test})
