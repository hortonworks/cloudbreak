NAME=cloudbreak-deployer
BINARYNAME=cbd
ARTIFACTS=LICENSE.txt NOTICE.txt VERSION README
ARCH=$(shell uname -m)
VERSION=$(shell cat VERSION)
GIT_REV=$(shell git rev-parse --short HEAD)
GIT_BRANCH=$(shell git rev-parse --abbrev-ref HEAD)
S3_TARGET?=s3://public-repo-1.hortonworks.com/HDP/cloudbreak/
#S3_TARGET=s3://public-repo.sequenceiq.com

ifeq ($(GIT_BRANCH), release)
FLAGS="-X main.Version $(VERSION)"
else
FLAGS="-X main.Version $(VERSION) -X main.GitRevision $(GIT_BRANCH)-$(GIT_REV)"
endif

build: bindata
	mkdir -p build/Linux  && GOOS=linux  go build -ldflags $(FLAGS) -o build/Linux/$(BINARYNAME)
	mkdir -p build/Darwin && GOOS=darwin go build -ldflags $(FLAGS) -o build/Darwin/$(BINARYNAME)

dev: bindata
	go build -ldflags $(FLAGS) -o /usr/local/bin/$(BINARYNAME)

dev-debug:
	go-bindata -debug=true include .deps/bin
	go build -ldflags $(FLAGS) -o /usr/local/bin/$(BINARYNAME)

bindata:
	go-bindata include .deps/bin

install: build
	install build/$(shell uname -s)/$(BINARYNAME) /usr/local/bin

deps:
	go get -u github.com/jteeuwen/go-bindata/...
	go get -u github.com/progrium/gh-release/...
	go get github.com/progrium/basht
	go get github.com/github/hub
	go get || true

tests:
	basht include/*.bash test/*.bash

integration-tests:
	basht include/*.bash integration-test/*.bash

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
	
release: prepare-release

	gh-release checksums sha256
	gh-release create sequenceiq/$(NAME) $(VERSION) $(GIT_BRANCH) v$(VERSION)
	@echo upload artifacts to $(S3_TARGET) ...
	@docker run \
		-v $(PWD):/data \
		-w /data \
		-e AWS_ACCESS_KEY_ID=$(AWS_ACCESS_KEY_ID) \
		-e AWS_SECRET_ACCESS_KEY=$(AWS_SECRET_ACCESS_KEY) \
		anigeo/awscli s3 cp release/ $(S3_TARGET) --recursive --include "$(NAME)_$(VERSION)_*.tgz"

release-next-ver: deps
	./release-next-ver.sh

circleci:
	rm ~/.gitconfig

clean:
	rm -rf build release

.PHONY: build release
