export MAVEN_METADATA_URL = maven.sequenceiq.com/releases/com/sequenceiq/cloudbreak/maven-metadata.xml
export DOCKER_IMAGE = hortonworks/cloudbreak
export FEDRAMP_DOCKER_IMAGE = hortonworks/cloudbreak-fedramp
export SCAN ?= false
export AQUASEC_DOCKER_IMAGE ?= docker-registry.infra.cloudera.com:5001/scanner:4.2

dockerhub:
	./deploy.sh $(VERSION)
ifeq ($(SCAN), true)
	./scan.sh $(VERSION)
endif