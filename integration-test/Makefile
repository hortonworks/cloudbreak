include integcb/Profile

INTEGCB_LOCATION := integcb

ifdef DOCKER_MACHINE
TEST_ENV := docker-machine
TMP := $(shell docker-machine env $(DOCKER_MACHINE) | awk '{ gsub ("\"", ""); print}' > docker_env.tmp)
include docker_env.tmp
else
TEST_ENV := boot2docker
endif

all: buildcb create-image create-cloudbreak-context docker-compose check-results

buildcb:
	@echo "\n\033[1;96m--- build cloudbreak\033[0m\n"
	../gradlew -p ../ clean build

create-image:
	@echo "\n\033[1;96m--- Test will run on \033[92;5m"$(TEST_ENV)"\033[0m\n"
	@echo docker-host: $(DOCKER_HOST)
	@echo "\n\033[1;96m--- Copy ../core/build/libs/cloudbreak.jar to test-image directory\033[0m\n"
	@cp ../core/build/libs/cloudbreak.jar test-image
	@echo "\n\033[1;96m--- Build dev cloudbreak test image\033[0m\n"
	@docker build -t sequenceiq/cloudbreak:dev test-image

create-cloudbreak-context:
	@echo "\n\033[1;96m--- Create cloudbreak context\033[0m\n"
	@cd $(INTEGCB_LOCATION); cbd delete;
	@cd $(INTEGCB_LOCATION); docker-compose down; cbd regenerate;
	@docker-compose down

docker-compose:
	@echo "\n\033[1;96m--- Stop and remove running cbd containers\033[0m\n"
	@cd $(INTEGCB_LOCATION); docker-compose down
#	@docker-compose down
	@echo "\n\033[1;96m--- Start cloudbreak\033[0m\n"
	@cd $(INTEGCB_LOCATION); docker-compose up -d consul registrator uaadb identity cbdb cloudbreak > /dev/null
	@rm -rf test-output
	@echo "\n\033[1;96m--- Check cloudbreak if available\033[0m\n"
	@docker run -it --rm --net=container:integcb_cloudbreak_1 --entrypoint /bin/bash sequenceiq/cloudbreak -c /wait_for_cloudbreak_api.sh
	@echo "\n\033[1;96m--- Start testing... (it may take few minutes to finish.)\033[0m\n"
	@docker-compose up test > test.out
	@echo "\n\033[1;96m--- Test finished\033[0m\n"

check-results:
	@if grep -q 'skipped="0".*failed="0"' test-output/testng-results.xml; then echo "\n\033[0;92m+++ INTEGRATION TEST SUCCESSFULLY FINISHED +++\n"; else echo "\033[0;91m--- !!! INTEGRATION TEST FAILED, CHECK \033[1;93mtest-output\033[0;91m DIR FOR RESULTS !!! ---\n"; fi

remove-test-containers:
	@cd $(INTEGCB_LOCATION); cbd kill; cbd delete;

without-build: create-image create-cloudbreak-context docker-compose check-results
