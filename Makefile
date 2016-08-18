NAME=cloudbreak-deployer
BINARYNAME=cbd
ARTIFACTS=LICENSE.txt NOTICE.txt VERSION README
ARCH=$(shell uname -m)
VERSION_FILE=$(shell cat VERSION)
GIT_REV=$(shell git rev-parse --short HEAD)
GIT_BRANCH=$(shell git rev-parse --abbrev-ref HEAD)
GIT_TAG=$(shell git describe --exact-match --tags 2>/dev/null )
S3_TARGET?=s3://public-repo-1.hortonworks.com/HDP/cloudbreak/
#S3_TARGET=s3://public-repo.sequenceiq.com

# if on a git tag, use that as a version number
ifeq ($(GIT_TAG),)
	  VERSION=$(VERSION_FILE)-$(GIT_BRANCH)
else
	  VERSION=$(GIT_TAG)
endif

# if on release branch dont use git revision
ifeq ($(GIT_BRANCH), release)
  FLAGS="-X main.Version $(VERSION)"
  VERSION=$(VERSION_FILE)
else
	FLAGS="-X main.Version $(VERSION) -X main.GitRevision $(GIT_REV)"
endif

echo_version:
	echo GIT_TAG[$(GIT_TAG)]
ifeq ($(GIT_TAG),)
	echo EMPTY TAG
 else
	echo NOT_EMPTY_TAG
endif

	echo VERSION=$(VERSION)

update-container-versions:
	sed -i "0,/DOCKER_TAG_PERISCOPE/ s/DOCKER_TAG_PERISCOPE .*/DOCKER_TAG_PERISCOPE $(CB_VERSION)/" include/cloudbreak.bash
	sed -i "0,/DOCKER_TAG_CLOUDBREAK/  s/DOCKER_TAG_CLOUDBREAK .*/DOCKER_TAG_CLOUDBREAK $(CB_VERSION)/" include/cloudbreak.bash
	sed -i "0,/DOCKER_TAG_ULUWATU/ s/DOCKER_TAG_ULUWATU .*/DOCKER_TAG_ULUWATU $(CB_VERSION)/" include/cloudbreak.bash
	sed -i "0,/DOCKER_TAG_SULTANS/ s/DOCKER_TAG_SULTANS .*/DOCKER_TAG_SULTANS $(CB_VERSION)/" include/cloudbreak.bash
	sed -i "0,/DOCKER_TAG_CLOUDBREAK_SHELL/ s/DOCKER_TAG_CLOUDBREAK_SHELL .*/DOCKER_TAG_CLOUDBREAK_SHELL $(CB_VERSION)/" include/cloudbreak.bash

push-container-versions: update-container-versions
	git add include/cloudbreak.bash
	git commit -m "Updated container versions to $(CB_VERSION)"
	git tag $(CB_VERSION)
	git push origin HEAD:$(GIT_BRANCH) --tags

build: bindata ## Creates linux an osx binaries in "build/$OS"
	mkdir -p build/Linux  && GOOS=linux  go build -ldflags $(FLAGS) -o build/Linux/$(BINARYNAME)
	mkdir -p build/Darwin && GOOS=darwin go build -ldflags $(FLAGS) -o build/Darwin/$(BINARYNAME)

create-snapshot-tgz: ## Creates snapshot tgz from binaries into snapshot dir
	rm -rf snapshots
	mkdir -p snapshots

	tar -czf snapshots/cloudbreak-deployer_snapshot_Linux_x86_64.tgz -C build/Linux cbd
	tar -czf snapshots/cloudbreak-deployer_snapshot_Darwin_x86_64.tgz -C build/Darwin cbd

upload-snapshot: create-snapshot-tgz
	@echo upload snapshot artifacts to $(S3_TARGET) ...
	@docker run \
		-v $(PWD):/data \
		-w /data \
		-e AWS_ACCESS_KEY_ID=$(AWS_ACCESS_KEY_ID) \
		-e AWS_SECRET_ACCESS_KEY=$(AWS_SECRET_ACCESS_KEY) \
		anigeo/awscli s3 cp snapshots/ $(S3_TARGET) --recursive --include "$(NAME)_$(VERSION)_*.tgz"
	rm -rf snapshots


dev: bindata
	go build -ldflags $(FLAGS) -o /usr/local/bin/$(BINARYNAME)

dev-debug: ## Installs dev version into /usr/local/bin. bash scripts are linked, so changes are effective without new build
	go-bindata -debug=true include .deps/bin
	go build -ldflags $(FLAGS) -o /usr/local/bin/$(BINARYNAME)

bindata:
	go-bindata include .deps/bin

install: build ## Installs OS specific binary into: /usr/local/bin
	install build/$(shell uname -s)/$(BINARYNAME) /usr/local/bin

deps: ## Installs required cli tools (only needed for new envs)
	go get -u github.com/jteeuwen/go-bindata/...
	go get -u github.com/progrium/gh-release/...
	go get github.com/progrium/basht
#	go get github.com/github/hub
	go get || true

tests:
	basht include/*.bash test/*.bash

integration-tests:
	basht include/*.bash integration-test/*.bash

integration-tests-centos:
	docker build -f Dockerfile.integrationtest -t cbd:integration .
	docker run -it --rm \
		-v $(PWD):$(PWD) \
		-w $(PWD) \
		-v /var/run/docker.sock:/var/run/docker.sock \
		-v /usr/local/bin/docker:/usr/local/bin/docker \
		-e CBD_TMPDIR=$(PWD)/delme-centos \
		cbd:integration make integration-tests

		cbd:integration make integration-tests

install-test:
	docker rmi cbd:delme 2>/dev/null || true
	docker build -f Dockerfile.installtest -t cbd:delme .
	docker run --rm cbd:delme cbd --version

prepare-release: build
	rm -rf release && mkdir release

	cp $(ARTIFACTS) build/Linux/
	tar -zcf release/$(NAME)_$(VERSION)_Linux_$(ARCH).tgz -C build/Linux $(ARTIFACTS) $(BINARYNAME)
	cp $(ARTIFACTS) build/Darwin/
	tar -zcf release/$(NAME)_$(VERSION)_Darwin_$(ARCH).tgz -C build/Darwin $(ARTIFACTS) $(BINARYNAME)

upload-release: prepare-release
	@echo upload artifacts to $(S3_TARGET) ...
	@docker run \
		-v $(PWD):/data \
		-w /data \
		-e AWS_ACCESS_KEY_ID=$(AWS_ACCESS_KEY_ID) \
		-e AWS_SECRET_ACCESS_KEY=$(AWS_SECRET_ACCESS_KEY) \
		anigeo/awscli s3 cp release/ $(S3_TARGET) --recursive --include "$(NAME)_$(VERSION)_*.tgz"

upload-tagged: prepare-release
ifeq ($(GIT_TAG),)
	@echo "not a tag, no upload needed"
else
	@echo upload artifacts to $(S3_TARGET) ...
	@docker run \
		-v $(PWD):/data \
		-w /data \
		-e AWS_ACCESS_KEY_ID=$(AWS_ACCESS_KEY_ID) \
		-e AWS_SECRET_ACCESS_KEY=$(AWS_SECRET_ACCESS_KEY) \
		anigeo/awscli s3 cp release/ $(S3_TARGET) --recursive --include "$(NAME)_$(VERSION)_*.tgz"
endif


release: upload-release
	gh-release checksums sha256
	gh-release create sequenceiq/$(NAME) $(VERSION) $(GIT_BRANCH) v$(VERSION)

release-next-ver: deps
	./release-next-ver.sh

generate-aws-json:
	curl -L https://atlas.hashicorp.com/api/v1/artifacts/sequenceiq/cbd/amazon.image/search | jq .versions[0] > mkdocs_theme/providers/aws.json

generate-openstack-json:
	curl -L  https://atlas.hashicorp.com/api/v1/artifacts/sequenceiq/cbd/openstack.image/search | jq .versions[0] > 
	mkdocs_theme/providers/openstack.json

circleci:
	rm ~/.gitconfig

clean:
	rm -rf build release

help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.PHONY: build release generate-aws-json help

.DEFAULT_GOAL := help



