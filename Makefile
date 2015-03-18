NAME=cloudbreak-deployer
BINARYNAME=cbd
ARCH=$(shell uname -m)
VERSION=$(shell cat VERSION)
GIT_REV=$(shell git rev-parse --short HEAD)
GIT_BRANCH=$(shell git rev-parse --abbrev-ref HEAD)

build:
	go-bindata include
	mkdir -p build/Linux  && GOOS=linux  go build -ldflags "-X main.Version $(VERSION) -X main.GitRevision $(GIT_REV)" -o build/Linux/$(BINARYNAME)
	mkdir -p build/Darwin && GOOS=darwin go build -ldflags "-X main.Version $(VERSION) -X main.GitRevision $(GIT_REV)" -o build/Darwin/$(BINARYNAME)

install: build
	install build/$(shell uname -s)/$(BINARYNAME) /usr/local/bin

deps:
	go get -u github.com/jteeuwen/go-bindata/...
	go get -u github.com/progrium/gh-release/...
	go get || true

release:
	rm -rf release && mkdir release
	tar -zcf release/$(NAME)_$(VERSION)_Linux_$(ARCH).tgz -C build/Linux $(BINARYNAME)
	tar -zcf release/$(NAME)_$(VERSION)_Darwin_$(ARCH).tgz -C build/Darwin $(BINARYNAME)
	gh-release checksums sha256
	gh-release create sequenceiq/$(NAME) $(VERSION) $(GIT_BRANCH) v$(VERSION)

circleci:
	rm ~/.gitconfig

clean:
	rm -rf build release

.PHONY: build release
