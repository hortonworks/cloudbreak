all: create-image create-cloudbreak-context create-certs docker-compose check-results

create-image:
	@echo "\n\033[1;96m--- Copy ../core/build/libs/cloudbreak.jar to test-image directory\033[0m\n"
	@cp ../core/build/libs/cloudbreak.jar test-image
	@echo "\n\033[1;96m--- Build dev cloudbreak test image\033[0m\n"
	@docker build -t sequenceiq/cloudbreak:dev test-image

create-cloudbreak-context:
	@echo "\n\033[1;96m--- Create cloudbreak context\033[0m\n"
	@cd cbd; cbd kill; cbd delete; cbd regenerate;
	@docker-compose down

create-certs:
	@echo "\n\033[1;96m--- Create certificates\033[0m\n"
	@docker run --rm -v $(shell pwd)/build/it-certs:/certs ehazlett/cert-tool:0.0.3 -d /certs -o=local > /dev/null

docker-compose:
	@echo "\n\033[1;96m--- Stop and remove running cbd containers if running\033[0m\n"
	@cd cbd; docker-compose down
#	@docker-compose down
	@echo "\n\033[1;96m--- Start cloudbreak\033[0m\n"
	@cd cbd; docker-compose up -d consul registrator uaadb identity cbdb cloudbreak > /dev/null
	@rm -rf test-output
	@echo "\n\033[1;96m--- Start testing... (it may take few minutes to finish.)\033[0m\n"
	@docker-compose up test > test.out
	@echo "\n\033[1;96m--- Test finished\033[0m\n"

check-results:
	@if grep -q 'skipped="0".*failed="0"' test-output/testng-results.xml; then echo "\n\033[0;92m+++ INTEGRATION TEST SUCCESSFULLY FINISHED +++\n"; else echo "\033[0;91m--- !!! INTEGRATION TEST FAILED, CHECK \033[1;93mtest-output\033[0;91m DIR FOR RESULTS !!! ---\n"; fi

remove-test-containers:
	@cd cbd; cbd kill; cbd delete;
