BINARY=hdc

VERSION=1.7.0-dev.17
BUILD_TIME=$(shell date +%FT%T)
LDFLAGS=-ldflags "-X github.com/hortonworks/hdc-cli/cli.Version=${VERSION} -X github.com/hortonworks/hdc-cli/cli.BuildTime=${BUILD_TIME}"
GOFILES_NOVENDOR = $(shell find . -type f -name '*.go' -not -path "./vendor/*")

deps:
	go get github.com/keyki/glu
	go get github.com/tools/godep

format:
	@gofmt -w ${GOFILES_NOVENDOR}

build: format build-darwin build-linux build-windows

build-darwin:
	GOOS=darwin go build -a -installsuffix cgo ${LDFLAGS} -o build/Darwin/${BINARY} main.go

build-linux:
	GOOS=linux go build -a -installsuffix cgo ${LDFLAGS} -o build/Linux/${BINARY} main.go

build-windows:
	GOOS=windows go build -a -installsuffix cgo ${LDFLAGS} -o build/Windows/${BINARY}.exe main.go

generate-swagger:
	swagger generate client -f http://localhost:9091/cb/api/v1/swagger.json

generate-swagger-docker:
	@docker run --rm -it -v "${GOPATH}":"${GOPATH}" -w "${PWD}" -e GOPATH --net=host quay.io/goswagger/swagger:0.5.0 \
	swagger generate client -f http://192.168.99.100:8080/cb/api/v1/swagger.json

release: build
	rm -rf release
	glu release

upload_s3:
	ls -1 release | xargs -I@ aws s3 cp release/@ s3:///hdc-cli/@ --acl public-read

linux-test: build-linux
	docker run --rm -it -v ${PWD}/build/Linux/hdc:/usr/sbin/hdc --name hdc alpine sh

.DEFAULT_GOAL := build
