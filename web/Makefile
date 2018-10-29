IMAGE = hortonworks/cb-web
VERSION ?= $(shell git  describe --tags --abbrev=0)
UNAME = $(shell uname)

dockerhub:
	./deploy.sh $(VERSION)
