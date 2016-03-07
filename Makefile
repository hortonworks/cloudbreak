dev-deploy:
	 ./gradlew clean build && scp ./core/build/libs/cloudbreak-*.jar cbd-test:

generate-image-yamls:
	 make -C cloud-openstack generate-yml
	 make -C cloud-arm generate-yml
	 make -C cloud-gcp generate-yml
	 make -C cloud-aws generate-yml
	 make -C cloud-common update-ambari-image

beautify:
	 auth/format.sh
	 web/format.sh

beautify-deps:
	 npm install -g js-beautify