#!/usr/bin/env bash

set -ex

LATEST_JACOCO_VERSION=$(curl -s https://search.maven.org/solrsearch/select?q=g:org.jacoco+AND+a:org.jacoco.cli | jq -r '.response.docs[0].latestVersion')
curl -sS https://nexus-private.eng.cloudera.com/nexus/content/groups/public/org/jacoco/org.jacoco.cli/${LATEST_JACOCO_VERSION}/org.jacoco.cli-${LATEST_JACOCO_VERSION}-nodeps.jar > jacococli.jar

java -jar jacococli.jar report ./integcb/jacoco-reports/cbreak_cloudbreak_1_jacoco.exec \
  --classfiles ../core/build/classes/java/main \
  --sourcefiles ../core/src/main/java \
  --xml ./integcb/jacoco-reports/cbreak_cloudbreak_1_jacoco_report.xml \
  --encoding UTF-8

java -jar jacococli.jar report ./integcb/jacoco-reports/cbreak_datalake_1_jacoco.exec \
  --classfiles ../datalake/build/classes/java/main \
  --sourcefiles ../datalake/src/main/java \
  --xml ./integcb/jacoco-reports/cbreak_datalake_1_jacoco_report.xml \
  --encoding UTF-8

java -jar jacococli.jar report ./integcb/jacoco-reports/cbreak_environment_1_jacoco.exec \
  --classfiles ../environment/build/classes/java/main \
  --sourcefiles ../environment/src/main/java \
  --xml ./integcb/jacoco-reports/cbreak_environment_1_jacoco_report.xml \
  --encoding UTF-8

java -jar jacococli.jar report ./integcb/jacoco-reports/cbreak_externalized-compute_1_jacoco.exec \
  --classfiles ../externalized-compute/build/classes/java/main \
  --sourcefiles ../externalized-compute/src/main/java \
  --xml ./integcb/jacoco-reports/cbreak_externalized-compute_1_jacoco_report.xml \
  --encoding UTF-8

java -jar jacococli.jar report ./integcb/jacoco-reports/cbreak_freeipa_1_jacoco.exec \
  --classfiles ../freeipa/build/classes/java/main \
  --sourcefiles ../freeipa/src/main/java \
  --xml ./integcb/jacoco-reports/cbreak_freeipa_1_jacoco_report.xml \
  --encoding UTF-8

java -jar jacococli.jar report ./integcb/jacoco-reports/cbreak_redbeams_1_jacoco.exec \
  --classfiles ../redbeams/build/classes/java/main \
  --sourcefiles ../redbeams/src/main/java \
  --xml ./integcb/jacoco-reports/cbreak_redbeams_1_jacoco_report.xml \
  --encoding UTF-8

java -jar jacococli.jar report ./integcb/jacoco-reports/cbreak_remote-environment_1_jacoco.exec \
  --classfiles ../environment-remote/build/classes/java/main \
  --sourcefiles ../environment-remote/src/main/java \
  --xml ./integcb/jacoco-reports/cbreak_remote-environment_1_jacoco_report.xml \
  --encoding UTF-8

java -jar jacococli.jar report ./integcb/jacoco-reports/cbreak_periscope_1_jacoco.exec \
  --classfiles ../autoscale/build/classes/java/main \
  --sourcefiles ../autoscale/src/main/java \
  --xml ./integcb/jacoco-reports/cbreak_periscope_1_jacoco_report.xml \
  --encoding UTF-8