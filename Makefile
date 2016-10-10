dev-deploy:
	 ./gradlew clean build && scp ./core/build/libs/cloudbreak-*.jar cbd-test:

generate-image-yamls:
	 make -C cloud-openstack generate-yml
	 make -C cloud-arm generate-yml
	 make -C cloud-gcp generate-yml
	 make -C cloud-aws generate-yml

generate-ambari-docker-image:
	 make -C cloud-common update-ambari-image

beautify:
	 ./gradlew jsformatter

generate-flow-graphs:
	 ./gradlew -PmainClass=com.sequenceiq.cloudbreak.core.flow2.config.OfflineStateGenerator -q :core:execute

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
