#!/usr/bin/env bash

curl ${REPO_URL}/com/sequenceiq/cloudbreak/${VERSION}/cloudbreak-${VERSION}.jar -o ../core/build/libs/cloudbreak.jar --create-dirs
curl ${REPO_URL}/com/sequenceiq/environment/${VERSION}/environment-${VERSION}.jar -o ../environment/build/libs/environment.jar --create-dirs
curl ${REPO_URL}/com/sequenceiq/redbeams/${VERSION}/redbeams-${VERSION}.jar -o ../redbeams/build/libs/redbeams.jar --create-dirs
curl ${REPO_URL}/com/sequenceiq/datalake/${VERSION}/datalake-${VERSION}.jar -o ../datalake/build/libs/datalake.jar --create-dirs
curl ${REPO_URL}/com/sequenceiq/freeipa/${VERSION}/freeipa-${VERSION}.jar -o ../freeipa/build/libs/freeipa.jar --create-dirs
curl ${REPO_URL}/com/sequenceiq/periscope/${VERSION}/periscope-${VERSION}.jar -o ../autoscale/build/libs/periscope.jar --create-dirs
curl ${REPO_URL}/com/sequenceiq/mock-thunderhead/${VERSION}/mock-thunderhead-${VERSION}.jar -o ../mock-thunderhead/build/libs/mock-thunderhead.jar --create-dirs
curl ${REPO_URL}/com/sequenceiq/cloudbreak-integration-test/${VERSION}/cloudbreak-integration-test-${VERSION}.jar -o ../integration-test/build/libs/cloudbreak-integration-test.jar --create-dirs
