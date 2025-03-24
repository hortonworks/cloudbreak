dev-deploy:
	 ./gradlew clean build && scp ./core/build/libs/cloudbreak-*.jar cbd-test:

generate-ambari-docker-image:
	 make -C cloud-common update-ambari-image

beautify:
	 ./gradlew jsformatter

generate-flow-graphs:
	 ./gradlew -PmainClassValue=com.sequenceiq.cloudbreak.core.flow2.generator.FlowOfflineStateGraphGenerator -q :core:execute
	 ./gradlew -PmainClassValue=com.sequenceiq.datalake.flow.graph.FlowOfflineStateGraphGenerator -q :datalake:execute
	 ./gradlew -PmainClassValue=com.sequenceiq.environment.environment.flow.generator.FlowOfflineStateGraphGenerator -q :environment:execute
	 ./gradlew -PmainClassValue=com.sequenceiq.freeipa.flow.graph.FlowOfflineStateGraphGenerator -q :freeipa:execute
	 ./gradlew -PmainClassValue=com.sequenceiq.externalizedcompute.flow.graph.FlowOfflineStateGraphGenerator -q :externalized-compute:execute
	 ./gradlew -PmainClassValue=com.sequenceiq.redbeams.flow.graph.FlowOfflineStateGraphGenerator -q :redbeams:execute
	 ./gradlew -PmainClassValue=com.sequenceiq.cloudbreak.rotation.flow.graph.FlowOfflineStateGraphGenerator -q :secret-rotation:execute

generate-flow-graph-pictures:
	 ./scripts/generate-flow-graph-pictures.sh

build-rc:
	 ./scripts/build-rc.sh

build-rc-patch:
	 ./scripts/build-rc-patch.sh

build-dev:
	 ./scripts/build-dev.sh

build-autoscale:
	 ./autoscale/scripts/build-dev.sh

build-core:
	 ./core/scripts/build-dev.sh

build-datalake:
	 ./datalake/scripts/build-dev.sh

build-environment:
	 ./environment/scripts/build-dev.sh

build-freeipa:
	 ./freeipa/scripts/build-dev.sh

build-redbeams:
	 ./redbeams/scripts/build-dev.sh

build-environment-remote:
	 ./environment-remote/scripts/build-dev.sh

build-integration-test:
	 ./integration-test/scripts/build-dev.sh

build-externalized-compute:
	 ./externalized-compute/scripts/build-dev.sh

build-mock-thunderhead:
	 ./mock-thunderhead/scripts/build-dev.sh

build-mock-infrastructure:
	 ./mock-infrastructure/scripts/build-dev.sh

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

jira-update-steps_update_jira_ticket:
	 ./.github/steps/jira-update-steps_update_jira_ticket.sh

unit-test-steps_gradle_build:
	 ./.github/steps/unit-test-steps_gradle_build.sh

checkstyle-main-steps_gradle_build:
	 ./.github/steps/checkstyle-main-steps_gradle_build.sh

checkstyle-test-steps_gradle_build:
	 ./.github/steps/checkstyle-test-steps_gradle_build.sh

spotbugs-test-steps_gradle_build:
	 ./.github/steps/spotbugs-test-steps_gradle_build.sh

spotbugs-main-steps_gradle_build:
	 ./.github/steps/spotbugs-main-steps_gradle_build.sh

swagger-compatiblity-steps_openapi_compatibility_check:
	 ./.github/steps/swagger-compatiblity-steps_openapi_compatibility_check.sh

component-test-steps_component_test:
	 ./.github/steps/component-test-steps_component_test.sh

integration-test-steps_integration_test:
	 ./.github/steps/integration-test-steps_integration_test.sh
