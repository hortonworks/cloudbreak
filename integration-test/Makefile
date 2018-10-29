include integcb/Environment

ifdef DOCKER_MACHINE
TMP := $(shell docker-machine env $(DOCKER_MACHINE) | awk '{ gsub ("\"", ""); print}' > docker_env.tmp)
$(shell echo export TEST_ENV=docker-machine >> docker_env.tmp)
include docker_env.tmp
else
$(shell echo export TEST_ENV=boot2docker > docker_env.tmp)
endif
ifeq ($(REPO_URL),"")
REPO_URL = https://cloudbreak-maven.s3.amazonaws.com/releases
endif

all: download-cbd buildcb runtest

runtest: create-image create-cloudbreak-context docker-compose stop-containers check-results

delete-and-run: download-cbd cbd-delete buildcb runtest

without-build: download-cbd runtest

build-with-docker: download-cbd docker-build runtest

download-jar-from-s3:
	curl ${REPO_URL}/com/sequenceiq/cloudbreak/$(VERSION)/cloudbreak-$(VERSION).jar -o ../core/build/libs/cloudbreak.jar --create-dirs
	curl ${REPO_URL}/com/sequenceiq/cloudbreak-integration-test/$(VERSION)/cloudbreak-integration-test-$(VERSION).jar -o ../integration-test/build/libs/cloudbreak-integration-test.jar --create-dirs

download-cbd:
	./scripts/download-cbd.sh

buildcb:
	./scripts/build-cb.sh

create-image:
	./scripts/create-image.sh

create-cloudbreak-context:
	./scripts/create-cloudbreak-context.sh

docker-compose:
	./scripts/docker-compose.sh

check-results:
	./scripts/check-results.sh

stop-containers:
	./scripts/stop-containers.sh

remove-test-containers:
	./scripts/remove-test-containers.sh

docker-build:
	./scripts/docker-build.sh

revert-db:
	./scripts/revert-db.sh

cbd-delete:
	./scripts/cbd-delete.sh

upload-s3:
	echo "Not supported"
