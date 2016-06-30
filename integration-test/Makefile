include integcb/Environment

INTEGCB_LOCATION := integcb

ifdef DOCKER_MACHINE
TEST_ENV := docker-machine
TMP := $(shell docker-machine env $(DOCKER_MACHINE) | awk '{ gsub ("\"", ""); print}' > docker_env.tmp)
include docker_env.tmp
else
TEST_ENV := boot2docker
endif

all: download-cbd buildcb runtest

runtest: create-image create-cloudbreak-context docker-compose check-results stop-containers

download-cbd:
	@echo "\n\033[1;96m--- download latest cbd\033[0m\n"
	@cd $(INTEGCB_LOCATION); curl -L s3.amazonaws.com/public-repo-1.hortonworks.com/HDP/cloudbreak/cloudbreak-deployer_snapshot_$$(uname)_x86_64.tgz | tar -xz

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
	@cp $(INTEGCB_LOCATION)/Profile_template $(INTEGCB_LOCATION)/Profile
	@./fill_public_ip.sh
	@cd $(INTEGCB_LOCATION); ./cbd delete;
	@cd $(INTEGCB_LOCATION); ./cbd regenerate;
	@$(INTEGCB_LOCATION)/.deps/bin/docker-compose stop; $(INTEGCB_LOCATION)/.deps/bin/docker-compose kill

docker-compose:
	@echo "\n\033[1;96m--- Kill running cbd containers\033[0m\n"
	@cd $(INTEGCB_LOCATION); ./cbd kill
	@echo "\n\033[1;96m--- Kill running test container\033[0m\n"
	@$(INTEGCB_LOCATION)/.deps/bin/docker-compose down
	@echo "\n\033[1;96m--- Start cloudbreak\033[0m\n"
	@cd $(INTEGCB_LOCATION); ./cbd start-wait consul registrator uaadb identity cbdb cloudbreak
	@rm -rf test-output
	@echo "\n\033[1;96m--- Start testing... (it may take few minutes to finish.)\033[0m\n"
	@$(INTEGCB_LOCATION)/.deps/bin/docker-compose up test > test.out
	@echo "\n\033[1;96m--- Test finished\033[0m\n"

check-results:
	@if grep -q 'skipped="0".*failed="0"' test-output/testng-results.xml; then echo "\n\033[0;92m+++ INTEGRATION TEST SUCCESSFULLY FINISHED +++\n"; else echo "\033[0;91m--- !!! INTEGRATION TEST FAILED, CHECK \033[1;93mtest-output\033[0;91m DIR FOR RESULTS !!! ---\n"; fi

stop-containers:
	@echo "\n\033[1;96m--- Stop cbd containers"
	@cd $(INTEGCB_LOCATION); .deps/bin/docker-compose stop;
	@echo "\n\033[1;96m--- Save cloudbreak log to cloudbreak.log file"
	@cd $(INTEGCB_LOCATION); .deps/bin/docker-compose logs --no-color cloudbreak >& ../cloudbreak.log;

remove-test-containers:
	@cd $(INTEGCB_LOCATION); ./cbd kill; ./cbd delete;

without-build: create-image create-cloudbreak-context docker-compose check-results

build-with-docker: download-cbd docker-build runtest

docker-build:
	@echo "\n\033[1;96m--- build cloudbreak in docker container\033[0m\n"
	docker run -i --rm $(ADDITIONAL_DOCKER_BUILD_COMMAND) -v $(shell pwd)/../:/tmp/prj:rw java:openjdk-8 /tmp/prj/gradlew -b /tmp/prj/build.gradle clean build
