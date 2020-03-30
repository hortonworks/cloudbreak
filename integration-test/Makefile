include integcb/Environment

ifeq ($(REPO_URL),"")
REPO_URL = https://cloudbreak-maven.s3.amazonaws.com/releases
endif

CB_VERSION = $(shell echo \${VERSION})
ifeq ($(CB_VERSION),)
CB_VERSION = $(shell git tag --points-at HEAD | sort -n | tail -1)
endif

CB_TARGET_BRANCH = $(shell echo \${TARGET_BRANCH})

all: download-cbd buildcb runtest

runtest: create-image create-cloudbreak-context docker-compose stop-containers check-results

delete-and-run: download-cbd cbd-delete buildcb runtest

without-build: download-cbd runtest

build-with-docker: download-cbd docker-build runtest

bring-up-schema: download-cbd create-image create-cloudbreak-context schema-migration

download-jar-from-s3:
	./scripts/download-artifacts.sh

download-cbd:
	CB_VERSION=$(CB_VERSION) CB_TARGET_BRANCH=$(CB_TARGET_BRANCH) ./scripts/download-cbd.sh

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

schema-migration:
	./scripts/schema-migration.sh

upload-s3:
	aws s3 cp ./apidefinitions/cb.json s3://cloudbreak-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/environment.json s3://environment-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/freeipa.json s3://freeipa-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/redbeams.json s3://redbeams-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/datalake.json s3://datalake-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/autoscale.json s3://autoscale-swagger/swagger-$(VERSION).json --acl public-read
