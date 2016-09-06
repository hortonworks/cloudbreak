BINARY=hdc

VERSION=1.6.0-dev.1
BUILD_TIME=$(shell date +%FT%T)
LDFLAGS=-ldflags "-X github.com/hortonworks/hdc-cli/cli.Version=${VERSION} -X github.com/hortonworks/hdc-cli/cli.BuildTime=${BUILD_TIME}"

deps:
	go get github.com/gliderlabs/glu
	go get github.com/tools/godep

format:
	gofmt -w .

build: format build-darwin build-linux

build-darwin:
	GOOS=darwin go build -a -installsuffix cgo ${LDFLAGS} -o build/Darwin/${BINARY} main.go

build-linux:
	GOOS=linux go build -a -installsuffix cgo ${LDFLAGS} -o build/Linux/${BINARY} main.go

generate-swagger:
	swagger generate client -f http://localhost:9091/cb/api/v1/swagger.json

release: build
	rm -rf release
	glu release

linux-test: build-linux
	docker run --rm -it -v ${PWD}/build/Linux/hdc:/usr/sbin/hdc --name hdc alpine sh

.PHONY: build
