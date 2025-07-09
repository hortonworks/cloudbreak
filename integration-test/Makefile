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

runtest-ums: create-image create-cloudbreak-context config-ums docker-compose stop-containers check-results

runtest-with-code-coverage: download-cbd create-image-with-code-coverage create-cloudbreak-context docker-compose stop-containers check-results

delete-and-run: download-cbd cbd-delete buildcb runtest

without-build: download-cbd runtest

without-build-with-ums: download-cbd runtest-ums

build-with-docker: download-cbd docker-build runtest

swagger-check: buildswagger swagger-compare

bring-up-schema: download-cbd create-image create-cloudbreak-context schema-migration

clear-ums-user-assignments: fetch-secrets-docker clean-up-ums-user-assignments-docker

download-jar-from-s3:
	./scripts/download-artifacts.sh

download-cbd:
	CB_VERSION=$(CB_VERSION) CB_TARGET_BRANCH=$(CB_TARGET_BRANCH) ./scripts/download-cbd.sh

buildcb:
	./scripts/build-cb.sh

buildswagger:
	./scripts/build-swagger.sh

clean-up-ums-user-assignments-docker:
	./scripts/clear-ums-user-assignments-docker.sh

create-image:
	./scripts/create-image.sh

create-image-with-code-coverage:
	./scripts/create-image-with-code-coverage.sh

create-cloudbreak-context:
	./scripts/create-cloudbreak-context.sh

fetch-secrets:
	./scripts/fetch-secrets.sh

fetch-secrets-docker:
	./scripts/fetch-ums-docker.sh

fetch-ums-docker-gh-action:
	./scripts/fetch-ums-docker-gh-action.sh

docker-compose:
	CB_VERSION=$(CB_VERSION) CB_TARGET_BRANCH=$(CB_TARGET_BRANCH) ./scripts/docker-compose.sh

docker-compose-after-config-change:
	CB_VERSION=$(CB_VERSION) CB_TARGET_BRANCH=$(CB_TARGET_BRANCH) ./scripts/docker-compose-after-config-change.sh

check-results:
	./scripts/check-results.sh

compress-results:
	./scripts/compress-results.sh

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

config-ums:
	./scripts/config-ums.sh

schema-migration:
	./scripts/schema-migration.sh

swagger-compare:
	CB_VERSION=$(CB_VERSION) CB_TARGET_BRANCH=$(CB_TARGET_BRANCH) ./scripts/openapi-check.sh

upload-s3:
	aws s3 cp ./apidefinitions/cb.json s3://cloudbreak-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/environment.json s3://environment-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/freeipa.json s3://freeipa-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/redbeams.json s3://redbeams-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/datalake.json s3://datalake-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/autoscale.json s3://autoscale-swagger/swagger-$(VERSION).json --acl public-read

start-integration-test:
	./scripts/start-integration-test.sh

collect-integration-test-code-coverage:
	./scripts/collect-integration-test-code-coverage.sh

create-xml-reports-from-code-coverage-results:
	./scripts/create-xml-reports-from-code-coverage-results.sh
