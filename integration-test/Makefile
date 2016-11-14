include integcb/Environment

ifdef DOCKER_MACHINE
TMP := $(shell docker-machine env $(DOCKER_MACHINE) | awk '{ gsub ("\"", ""); print}' > docker_env.tmp)
$(shell echo export TEST_ENV=docker-machine >> docker_env.tmp)
include docker_env.tmp
else
$(shell echo export TEST_ENV=boot2docker > docker_env.tmp)
endif

all: download-cbd buildcb runtest

runtest: create-image create-cloudbreak-context docker-compose stop-containers check-results

without-build: download-cbd runtest

build-with-docker: download-cbd docker-build runtest

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
