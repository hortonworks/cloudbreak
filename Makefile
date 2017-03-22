BINARY=hdc

VERSION ?= snapshot
BUILD_TIME=$(shell date +%FT%T)
LDFLAGS=-ldflags "-X github.com/hortonworks/hdc-cli/cli.Version=${VERSION} -X github.com/hortonworks/hdc-cli/cli.BuildTime=${BUILD_TIME}"
GOFILES_NOVENDOR = $(shell find . -type f -name '*.go' -not -path "./vendor/*")
CB_IP = $(shell echo \${IP})
ifeq ($(CB_IP),)
        CB_IP = 192.168.99.100
endif

deps:
	go get github.com/tools/godep

format:
	@gofmt -w ${GOFILES_NOVENDOR}

vet:
	go vet -race github.com/hortonworks/hdc-cli/cli

test:
	go test -race github.com/hortonworks/hdc-cli/cli

coverage:
	go test github.com/hortonworks/hdc-cli/cli -cover

coverage-html:
	go test github.com/hortonworks/hdc-cli/cli -coverprofile fmt
	@go tool cover -html=fmt
	@rm -f fmt

build: format vet test build-darwin build-linux build-windows

build-docker:
	@#USER_NS='-u $(shell id -u $(whoami)):$(shell id -g $(whoami))'
	docker run --rm ${USER_NS} -v "${PWD}":/go/src/github.com/hortonworks/hdc-cli -w /go/src/github.com/hortonworks/hdc-cli -e VERSION=${VERSION} golang:1.8 make build

build-darwin:
	GOOS=darwin CGO_ENABLED=0 go build -a ${LDFLAGS} -o build/Darwin/${BINARY} main.go

build-linux:
	GOOS=linux CGO_ENABLED=0 go build -a ${LDFLAGS} -o build/Linux/${BINARY} main.go

build-windows:
	GOOS=windows CGO_ENABLED=0 go build -a ${LDFLAGS} -o build/Windows/${BINARY}.exe main.go

generate-swagger:
	swagger generate client -f http://$(CB_IP):8080/cb/api/v1/swagger.json -c client_cloudbreak -m models_cloudbreak

generate-swagger-docker:
	@docker run --rm -it -v "${GOPATH}":"${GOPATH}" -w "${PWD}" -e GOPATH --net=host quay.io/goswagger/swagger:0.5.0 \
	swagger generate client -f http://$(CB_IP):8080/cb/api/v1/swagger.json -c client_cloudbreak -m models_cloudbreak

generate-swagger-autoscale:
	swagger generate client -f http://$(CB_IP):8085/as/api/v1/swagger.json -c client_autoscale -m models_autoscale

generate-swagger-autoscale-docker:
	@docker run --rm -it -v "${GOPATH}":"${GOPATH}" -w "${PWD}" -e GOPATH --net=host quay.io/goswagger/swagger:0.5.0 \
	swagger generate client -f http://$(CB_IP):8085/as/api/v1/swagger.json -c client_autoscale -m models_autoscale

release: build
	rm -rf release
	mkdir release
	tar -zcvf release/hdc-cli_${VERSION}_Darwin_x86_64.tgz -C build/Darwin hdc
	tar -zcvf release/hdc-cli_${VERSION}_Linux_x86_64.tgz -C build/Linux hdc
	tar -zcvf release/hdc-cli_${VERSION}_Windows_x86_64.tgz -C build/Windows hdc.exe

release-docker:
	docker run --rm -v "${PWD}":/go/src/github.com/hortonworks/hdc-cli -w /go/src/github.com/hortonworks/hdc-cli -e VERSION=${VERSION} -e GITHUB_ACCESS_TOKEN=${GITHUB_TOKEN} golang:1.8 bash -c "make deps && make release"

upload_s3:
	ls -1 release | xargs -I@ aws s3 cp release/@ s3://hdc-cli/@ --acl public-read

linux-test: build-linux
	docker run --rm -it -v ${PWD}/build/Linux/:/usr/sbin/ --name hdc alpine sh

.DEFAULT_GOAL := build

.PHONY: build release
