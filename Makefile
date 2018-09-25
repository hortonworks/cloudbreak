dev-deploy:
	 ./gradlew clean build && scp ./core/build/libs/cloudbreak-*.jar cbd-test:

generate-ambari-docker-image:
	 make -C cloud-common update-ambari-image

beautify:
	 ./gradlew jsformatter

generate-flow-graphs:
	 ./gradlew -PmainClass=com.sequenceiq.cloudbreak.core.flow2.config.generator.OfflineStateGenerator -q :core:execute

build-rc:
	 ./scripts/build-rc.sh

build-rc-patch:
	 ./scripts/build-rc-patch.sh

build-dev:
	 ./scripts/build-dev.sh

build-dev-pipeline-poc:
	 ./scripts/build-dev-pipeline-poc.sh

build:
	 ./scripts/build.sh

build-release:
	 ./scripts/build-release.sh

git-secrets:
	brew install git-secrets
	git secrets --install
