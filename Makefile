BINARY=cb

VERSION ?= $(shell git describe --tags --abbrev=0)-snapshot
BUILD_TIME=$(shell date +%FT%T)
LDFLAGS=-ldflags "-X github.com/hortonworks/cb-cli/cloudbreak/common.Version=${VERSION} -X github.com/hortonworks/cb-cli/cloudbreak/common.BuildTime=${BUILD_TIME}"
LDFLAGS_NOVER=-ldflags "-X github.com/hortonworks/cb-cli/cloudbreak/common.Version=snapshot -X github.com/hortonworks/cb-cli/cloudbreak/common.BuildTime=${BUILD_TIME}"
GOFILES_NOVENDOR = $(shell find . -type f -name '*.go' -not -path "./vendor/*" -not -path "./.git/*")
CB_IP = $(shell echo \${IP})
ifeq ($(CB_IP),)
        CB_IP = 192.168.64.1
endif
CB_PORT = $(shell echo \${PORT})
ifeq ($(CB_PORT),)
        CB_PORT = 9091
endif

deps: deps-errcheck
	go get -u github.com/golang/dep/cmd/dep
	go get -u golang.org/x/tools/cmd/goimports
	curl -o $(GOPATH)/bin/swagger -L'#' https://github.com/go-swagger/go-swagger/releases/download/0.12.0/swagger_$(shell echo `uname`|tr '[:upper:]' '[:lower:]')_amd64
	chmod +x $(GOPATH)/bin/swagger

deps-errcheck:
	go get -u github.com/kisielk/errcheck

formatcheck:
	([ -z "$(shell gofmt -d $(GOFILES_NOVENDOR))" ]) || (echo "Source is unformatted"; exit 1)

format:
	@gofmt -w ${GOFILES_NOVENDOR}

vet:
	go vet -shadow ./...

test:
	go test -timeout 30s -race ./...

errcheck:
#	Module support is not on master yet: https://github.com/kisielk/errcheck/issues/152#issuecomment-415945206
#	errcheck -ignoretests -exclude errcheck_excludes.txt ./...

coverage:
	go test github.com/hortonworks/cb-cli/cloudbreak -cover

coverage-html:
	go test github.com/hortonworks/cb-cli/cloudbreak -coverprofile fmt
	@go tool cover -html=fmt
	@rm -f fmt

build: errcheck formatcheck vet test build-darwin build-linux build-windows

build-version: errcheck format vet test build-darwin-version build-linux-version build-windows-version

build-docker:
	@#USER_NS='-u $(shell id -u $(whoami)):$(shell id -g $(whoami))'
	docker run --rm ${USER_NS} -v "${PWD}":/go/src/github.com/hortonworks/cb-cli -w /go/src/github.com/hortonworks/cb-cli -e VERSION=${VERSION} -e GO111MODULE=on golang:1.11 make deps-errcheck build

build-darwin:
	GOOS=darwin GO111MODULE=on CGO_ENABLED=0 go build -a ${LDFLAGS_NOVER} -o build/Darwin/${BINARY} main.go

dev-debug-darwin:
	GOOS=darwin GO111MODULE=on CGO_ENABLED=0 go build -a ${LDFLAGS_NOVER} -o /usr/local/bin/${BINARY} main.go

build-linux:
	GOOS=linux GO111MODULE=on CGO_ENABLED=0 go build -a ${LDFLAGS_NOVER} -o build/Linux/${BINARY} main.go

build-windows:
	GOOS=windows GO111MODULE=on CGO_ENABLED=0 go build -a ${LDFLAGS_NOVER} -o build/Windows/${BINARY}.exe main.go

build-darwin-version:
	GOOS=darwin GO111MODULE=on CGO_ENABLED=0 go build -a ${LDFLAGS} -o build/Darwin/${BINARY} main.go

build-linux-version:
	GOOS=linux GO111MODULE=on CGO_ENABLED=0 go build -a ${LDFLAGS} -o build/Linux/${BINARY} main.go

build-windows-version:
	GOOS=windows GO111MODULE=on CGO_ENABLED=0 go build -a ${LDFLAGS} -o build/Windows/${BINARY}.exe main.go

generate-swagger: build-swagger-fix
	rm -rf cloudbreak/api/client cloudbreak/api/model
	swagger generate client -f http://$(CB_IP):$(CB_PORT)/cb/api/swagger.json -c client -m model -t cloudbreak/api
	make fix-swagger

generate-swagger-docker: build-swagger-fix
	rm -rf cloudbreak/api/client cloudbreak/api/model
	@docker run --rm -it -v "${GOPATH}":"${GOPATH}" -w "${PWD}" -e GOPATH --net=host quay.io/goswagger/swagger:0.12.0 \
	generate client -f http://$(CB_IP):$(CB_PORT)/cb/api/swagger.json -c client -m model -t cloudbreak/api
	make fix-swagger

build-swagger-fix:
	go build -o build/swagger_fix swagger_fix/main.go

fix-swagger:
	$(info fixed on master https://github.com/go-swagger/go-swagger/issues/1197#issuecomment-335610396)
	build/swagger_fix --src cloudbreak/api/model/platform_gateways_response.go --operation remove-statement --exp validateGateways:range-0,for-0,if-1
	build/swagger_fix --src cloudbreak/api/model/platform_ip_pools_response.go --operation remove-statement --exp validateIppools:range-0,for-0,if-1
	build/swagger_fix --src cloudbreak/api/model/platform_networks_response.go --operation remove-statement --exp validateNetworks:range-0,for-0,if-1
	build/swagger_fix --src cloudbreak/api/model/platform_ssh_keys_response.go --operation remove-statement --exp validateSSHKeys:range-0,for-0,if-1
	build/swagger_fix --src cloudbreak/api/model/platform_security_groups_response.go --operation remove-statement --exp validateSecurityGroups:range-0,for-0,if-1
	goimports -l -w cloudbreak/api/model
	goimports -l -w cloudbreak/api/client
	@gofmt -w ${GOFILES_NOVENDOR}

release: build
	rm -rf release
	mkdir release
	git tag v${VERSION}
	git push https://${GITHUB_ACCESS_TOKEN}@github.com/hortonworks/cb-cli.git v${VERSION}
	tar -zcvf release/cb-cli_${VERSION}_Darwin_x86_64.tgz -C build/Darwin "${BINARY}"
	tar -zcvf release/cb-cli_${VERSION}_Linux_x86_64.tgz -C build/Linux "${BINARY}"
	tar -zcvf release/cb-cli_${VERSION}_Windows_x86_64.tgz -C build/Windows "${BINARY}.exe"

release-version: build-version
	rm -rf release
	mkdir release
	git tag v${VERSION}
	git push https://${GITHUB_ACCESS_TOKEN}@github.com/hortonworks/cb-cli.git v${VERSION}
	tar -zcvf release/cb-cli_${VERSION}_Darwin_x86_64.tgz -C build/Darwin "${BINARY}"
	tar -zcvf release/cb-cli_${VERSION}_Linux_x86_64.tgz -C build/Linux "${BINARY}"
	tar -zcvf release/cb-cli_${VERSION}_Windows_x86_64.tgz -C build/Windows "${BINARY}.exe"

release-docker:
	@USER_NS='-u $(shell id -u $(whoami)):$(shell id -g $(whoami))'
	docker run --rm ${USER_NS} -v "${PWD}":/go/src/github.com/hortonworks/cb-cli -w /go/src/github.com/hortonworks/cb-cli -e VERSION=${VERSION} -e GITHUB_ACCESS_TOKEN=${GITHUB_TOKEN} -e GO111MODULE=on golang:1.11 bash -c "make deps && make release"

release-docker-version:
	@USER_NS='-u $(shell id -u $(whoami)):$(shell id -g $(whoami))'
	docker run --rm ${USER_NS} -v "${PWD}":/go/src/github.com/hortonworks/cb-cli -w /go/src/github.com/hortonworks/cb-cli -e VERSION=${VERSION} -e GITHUB_ACCESS_TOKEN=${GITHUB_TOKEN} -e GO111MODULE=on golang:1.11 bash -c "make deps && make release-version"

upload_s3:
	ls -1 release | xargs -I@ aws s3 cp release/@ s3://cb-cli/@ --acl public-read

linux-test: build-linux
	docker run --rm -it -v ${PWD}/build/Linux/:/usr/sbin/ --name "${BINARY}" alpine sh

integration-test: build-docker
	make -C tests all

e2e-test:
	make -C tests docker-aruba-test

.DEFAULT_GOAL := build

.PHONY: build release
