IMAGE = hortonworks/cloudbreak-mock
VERSION ?= $(shell git  describe --tags --abbrev=0)
BRANCH ?=$(shell git branch | grep \* | cut -d ' ' -f2)
UNAME = $(shell uname)

deps:
	curl -Ls https://github.com/progrium/dockerhub-tag/releases/download/v0.2.0/dockerhub-tag_0.2.0_$(UNAME)_x86_64.tgz | tar -xzC /usr/local/bin

dockerhub-tag:
	dockerhub-tag set $(IMAGE) $(VERSION) $(VERSION) /mock/cloudbreak

generate-api:
	./swagger-codegen.sh