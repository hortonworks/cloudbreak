IMAGE = hortonworks/cloudbreak-mock
VERSION ?= $(shell git  describe --tags --abbrev=0)
BRANCH ?=$(shell git branch | grep \* | cut -d ' ' -f2)
UNAME = $(shell uname)

deps:
	curl -Ls https://github.com/keyki/dockerhub-tag/releases/download/v0.2.1/dockerhub-tag_0.2.1_$(uname)_x86_64.tgz | tar -xzC /usr/local/bin

dockerhub-tag:
	docker build -t ${IMAGE}:${VERSION} .
	docker push ${IMAGE}:${VERSION}
	docker rmi ${IMAGE}:${VERSION}

generate-api:
	./swagger-codegen.sh
