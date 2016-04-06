dev-deploy:
	 ./gradlew clean build && scp ./core/build/libs/cloudbreak-*.jar cbd-test:

generate-image-yamls:
	 make -C cloud-openstack generate-yml
	 make -C cloud-arm generate-yml
	 make -C cloud-gcp generate-yml
	 make -C cloud-aws generate-yml
	 make -C cloud-common update-ambari-image

beautify:
	 ./gradlew jsformatter

generate-flow-transitions:
	 ./gradlew -PmainClass=com.sequenceiq.cloudbreak.core.flow2.config.OfflineStateGenerator -q execute | grep '::'