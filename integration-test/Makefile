include integcb/Environment

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
	curl ${REPO_URL}/com/sequenceiq/environment/$(VERSION)/environment-$(VERSION).jar -o ../environment/build/libs/environment.jar --create-dirs
	curl ${REPO_URL}/com/sequenceiq/redbeams/$(VERSION)/redbeams-$(VERSION).jar -o ../redbeams/build/libs/redbeams.jar --create-dirs
    curl ${REPO_URL}/com/sequenceiq/datalake/$(VERSION)/datalake-$(VERSION).jar -o ../datalake/build/libs/datalake.jar --create-dirs
    curl ${REPO_URL}/com/sequenceiq/freeipa/$(VERSION)/freeipa-$(VERSION).jar -o ../freeipa/build/libs/freeipa.jar --create-dirs
    curl ${REPO_URL}/com/sequenceiq/periscope/$(VERSION)/periscope-$(VERSION).jar -o ../autoscale/build/libs/periscope.jar --create-dirs
    curl ${REPO_URL}/com/sequenceiq/mock-caas/$(VERSION)/mock-caas-$(VERSION).jar -o ../mock-caas/build/libs/mock-caas.jar --create-dirs
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
	aws s3 cp ./apidefinitions/new.json s3://cloudbreak-swagger/swagger-$(VERSION).json --acl public-read
