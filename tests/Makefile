define run_test
    @echo ::: ${1} tests have been launched :::
    bats --tap e2e/${1}.bats | tee e2e/${1}-result.tap
endef

all: cbm integration-test

cbm: 
	./scripts/cbm.sh

integration-test:
	./scripts/integration-test.sh

e2e-test:
	./scripts/e2e-test.sh

run-test:
	$(call run_test,${test})
