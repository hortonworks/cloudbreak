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

delete-and-run: download-cbd cbd-delete buildcb runtest

without-build: download-cbd runtest

without-build-with-ums: download-cbd runtest-ums

build-with-docker: download-cbd docker-build runtest

swagger-check: download-cbd buildswagger create-cloudbreak-context swagger-compare

bring-up-schema: download-cbd create-image create-cloudbreak-context schema-migration

swagger-check: swagger-compare

download-jar-from-s3:
	./scripts/download-artifacts.sh

download-cbd:
	CB_VERSION=$(CB_VERSION) CB_TARGET_BRANCH=$(CB_TARGET_BRANCH) ./scripts/download-cbd.sh

buildcb:
	./scripts/build-cb.sh

buildswagger:
	./scripts/build-swagger.sh

create-image:
	./scripts/create-image.sh

create-cloudbreak-context:
	./scripts/create-cloudbreak-context.sh

fetch-secrets:
	./scripts/fetch-secrets.sh

fetch-l0-secrets:
	SECRET_VERSION=9793d2c1afa84cbb9fdfeaee2f4327db USER_JSON_SECRET=l0-ums-users-dev USER_JSON_LOCATION=./src/main/resources/ums-users/l0-api-credentials.json ./scripts/fetch-secrets.sh

fetch-secrets-docker:
	docker run --rm -v "${PWD}/:/tmp/" \
	-e AZURE_CLIENT_ID=${AZURE_CLIENT_ID} \
	-e AZURE_CLIENT_SECRET=${AZURE_CLIENT_SECRET} \
	-e AZURE_TENANT_ID=${AZURE_TENANT_ID} \
	-e AZURE_SUBSCRIPTION_ID=${AZURE_SUBSCRIPTION_ID} \
	docker-private.infra.cloudera.com/cloudera_thirdparty/mcr.microsoft.com/azure-cli:1.0 /bin/bash -c 'cd /tmp/ && ./scripts/fetch-secrets.sh'

docker-compose:
	CB_VERSION=$(CB_VERSION) ./scripts/docker-compose.sh

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

config-ums:
	./scripts/config-ums.sh

schema-migration:
	./scripts/schema-migration.sh

swagger-compare:
	CB_VERSION=$(CB_VERSION) CB_TARGET_BRANCH=$(CB_TARGET_BRANCH) ./scripts/swagger-check.sh

upload-s3:
	aws s3 cp ./apidefinitions/cb.json s3://cloudbreak-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/environment.json s3://environment-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/freeipa.json s3://freeipa-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/redbeams.json s3://redbeams-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/datalake.json s3://datalake-swagger/swagger-$(VERSION).json --acl public-read
	aws s3 cp ./apidefinitions/autoscale.json s3://autoscale-swagger/swagger-$(VERSION).json --acl public-read
