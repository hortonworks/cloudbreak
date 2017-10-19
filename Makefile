BINARY=cb

VERSION ?= snapshot
BUILD_TIME=$(shell date +%FT%T)
LDFLAGS=-ldflags "-X github.com/hortonworks/cb-cli/cli.Version=${VERSION} -X github.com/hortonworks/cb-cli/cli.BuildTime=${BUILD_TIME}"
GOFILES_NOVENDOR = $(shell find . -type f -name '*.go' -not -path "./vendor/*")
CB_IP = $(shell echo \${IP})
ifeq ($(CB_IP),)
        CB_IP = 192.168.64.1
endif
CB_PORT = $(shell echo \${PORT})
ifeq ($(CB_PORT),)
        CB_PORT = 9091
endif

deps:
	go get -u github.com/golang/dep/cmd/dep
	curl -o $(GOPATH)/bin/swagger -L'#' https://github.com/go-swagger/go-swagger/releases/download/$(shell curl -s https://api.github.com/repos/go-swagger/go-swagger/releases/latest | jq -r .tag_name)/swagger_$(shell echo `uname`|tr '[:upper:]' '[:lower:]')_amd64
	chmod +x $(GOPATH)/bin/swagger

format:
	@gofmt -w ${GOFILES_NOVENDOR}

vet:
	go vet ./...

test:
	go test -race ./...

coverage:
	go test github.com/hortonworks/cb-cli/cli -cover

coverage-html:
	go test github.com/hortonworks/cb-cli/cli -coverprofile fmt
	@go tool cover -html=fmt
	@rm -f fmt

build: format vet test build-darwin build-linux build-windows

build-docker:
	@#USER_NS='-u $(shell id -u $(whoami)):$(shell id -g $(whoami))'
	docker run --rm ${USER_NS} -v "${PWD}":/go/src/github.com/hortonworks/cb-cli -w /go/src/github.com/hortonworks/cb-cli -e VERSION=${VERSION} golang:1.9 make build

build-darwin:
	GOOS=darwin CGO_ENABLED=0 go build -a ${LDFLAGS} -o build/Darwin/${BINARY} main.go

build-linux:
	GOOS=linux CGO_ENABLED=0 go build -a ${LDFLAGS} -o build/Linux/${BINARY} main.go

build-windows:
	GOOS=windows CGO_ENABLED=0 go build -a ${LDFLAGS} -o build/Windows/${BINARY}.exe main.go

generate-swagger:
	swagger generate client -f http://$(CB_IP):$(CB_PORT)/cb/api/v1/swagger.json -c client_cloudbreak -m models_cloudbreak
	make fix-swagger

generate-swagger-docker:
	@docker run --rm -it -v "${GOPATH}":"${GOPATH}" -w "${PWD}" -e GOPATH --net=host quay.io/goswagger/swagger:0.12.0 \
	generate client -f http://$(CB_IP):$(CB_PORT)/cb/api/v1/swagger.json -c client_cloudbreak -m models_cloudbreak
	make fix-swagger

fix-swagger:
	$(info fixed on master https://github.com/go-swagger/go-swagger/issues/1197#issuecomment-335610396)
	go run swagger_fix/main.go --src models_cloudbreak/platform_gateways_response.go --operation remove-statement --exp validateGateways:range-0,for-0,if-1
	go run swagger_fix/main.go --src models_cloudbreak/platform_ip_pools_response.go --operation remove-statement --exp validateIppools:range-0,for-0,if-1
	go run swagger_fix/main.go --src client_cloudbreak/connectors/get_platform_networks_responses.go --operation remove-statement --exp Validate:range-0,for-0,if-1
	go run swagger_fix/main.go --src client_cloudbreak/connectors/get_platform_s_sh_keys_responses.go --operation remove-statement --exp Validate:range-0,for-0,if-1
	go run swagger_fix/main.go --src client_cloudbreak/connectors/get_platform_security_groups_responses.go --operation remove-statement --exp Validate:range-0,for-0,if-1
	goimports -l -w models_cloudbreak
	goimports -l -w client_cloudbreak
	@gofmt -w ${GOFILES_NOVENDOR}

generate-swagger-autoscale:
	swagger generate client -f http://$(CB_IP):8085/as/api/v1/swagger.json -c client_autoscale -m models_autoscale

generate-swagger-autoscale-docker:
	@docker run --rm -it -v "${GOPATH}":"${GOPATH}" -w "${PWD}" -e GOPATH --net=host quay.io/goswagger/swagger:0.11.0 \
	generate client -f http://$(CB_IP):8085/as/api/v1/swagger.json -c client_autoscale -m models_autoscale

release: build
	rm -rf release
	mkdir release
	git tag v${VERSION}
	git push https://${GITHUB_ACCESS_TOKEN}@github.com/hortonworks/cb-cli.git v${VERSION}
	tar -zcvf release/cb-cli_${VERSION}_Darwin_x86_64.tgz -C build/Darwin "${BINARY}"
	tar -zcvf release/cb-cli_${VERSION}_Linux_x86_64.tgz -C build/Linux "${BINARY}"
	tar -zcvf release/cb-cli_${VERSION}_Windows_x86_64.tgz -C build/Windows "${BINARY}.exe"

release-docker:
	docker run --rm -v "${PWD}":/go/src/github.com/hortonworks/cb-cli -w /go/src/github.com/hortonworks/cb-cli -e VERSION=${VERSION} -e GITHUB_ACCESS_TOKEN=${GITHUB_TOKEN} golang:1.9 bash -c "make deps && make release"

upload_s3:
	ls -1 release | xargs -I@ aws s3 cp release/@ s3://cb-cli/@ --acl public-read

linux-test: build-linux
	docker run --rm -it -v ${PWD}/build/Linux/:/usr/sbin/ --name "${BINARY}" alpine sh

.DEFAULT_GOAL := build

.PHONY: build release
