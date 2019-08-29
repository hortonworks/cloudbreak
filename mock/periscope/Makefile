IMAGE = hortonworks/periscope-mock
INTERNAL_IMAGE_NAME=${INTERNAL_IMAGE_NAME:-${IMAGE}}
VERSION ?= $(shell git  describe --tags --abbrev=0)
BRANCH ?=$(shell git branch | grep \* | cut -d ' ' -f2)
UNAME = $(shell uname)

deps:
	curl -Ls https://github.com/keyki/dockerhub-tag/releases/download/v0.2.1/dockerhub-tag_0.2.1_$(UNAME)_x86_64.tgz | tar -xzC /usr/local/bin

dockerhub-tag:
	docker build -t ${IMAGE}:${VERSION} .
	docker push ${IMAGE}:${VERSION}
        docker tag ${IMAGE}:${VERSION} ${INTERNAL_IMAGE_NAME}:${VERSION}
        docker push ${INTERNAL_IMAGE_NAME}:${VERSION}	
        docker rmi ${IMAGE}:${VERSION}
        docker rmi ${INTERNAL_IMAGE_NAME}:${VERSION}
