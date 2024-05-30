dev-deploy:
	 ./gradlew clean build && scp ./core/build/libs/cloudbreak-*.jar cbd-test:

generate-ambari-docker-image:
	 make -C cloud-common update-ambari-image

beautify:
	 ./gradlew jsformatter

generate-flow-graphs:
	 ./gradlew -PmainClass=com.sequenceiq.flow.core.config.generator.OfflineStateGenerator -q :core:execute

build-rc:
	 ./scripts/build-rc.sh

build-rc-patch:
	 ./scripts/build-rc-patch.sh

build-dev:
	 ./scripts/build-dev.sh

build:
	 ./scripts/build.sh

build-release:
	 ./scripts/build-release.sh

git-secrets:
	brew install git-secrets
	git secrets --install

build-cloudbreak-dockerhub:
	 make -C docker-cloudbreak dockerhub

build-autoscale-dockerhub:
	 make -C docker-autoscale dockerhub

build-datalake-dockerhub:
	 make -C docker-datalake dockerhub

build-redbeams-dockerhub:
	 make -C docker-redbeams dockerhub

build-environment-dockerhub:
	 make -C docker-environment dockerhub

build-freeipa-dockerhub:
	 make -C docker-freeipa dockerhub

enable-gitconfig-local:
	 git config --local include.path ../.gitconfig

import-redbeams-aws-rds-root-certs:
	make -C redbeams import-aws-rds-root-certs
