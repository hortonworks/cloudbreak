#!/usr/bin/env bash

set -ex

docker login --username=$DOCKERHUB_USERNAME --password=$DOCKERHUB_PASSWORD
rm -rf ./integcb/docker-containers
mkdir -p ./integcb/docker-containers

folder="commercial"
special_build_args="--build-arg FIPS_MODE_ENABLED=false"
if [[ "$USE_FEDRAMP_CONTAINERS" == true ]]; then
  folder="fedramp"
fi

date
echo -e "\n\033[1;96m--- Copy ../core/build/libs/cloudbreak.jar to docker-cloudbreak directory\033[0m\n"
cp -R ../docker-cloudbreak/ ./integcb/docker-containers/docker-cloudbreak/
cp ../core/build/libs/cloudbreak.jar ./integcb/docker-containers/docker-cloudbreak
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-cloudbreak:/integcb/docker-containers/docker-cloudbreak \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i '/cloudbreak-\$VERSION/c\ADD cloudbreak.jar /\nADD jacocoagent.jar /' /integcb/docker-containers/docker-cloudbreak/$folder/Dockerfile"
echo "StatusCode######## $?"

date
echo -e "\n\033[1;96m--- Copy ../autoscale/build/libs/periscope.jar to docker-autoscale directory\033[0m\n"
cp -R ../docker-autoscale/ ./integcb/docker-containers/docker-autoscale/
cp ../autoscale/build/libs/periscope.jar ./integcb/docker-containers/docker-autoscale
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-autoscale:/integcb/docker-containers/docker-autoscale \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i '/periscope-\$VERSION/c\ADD periscope.jar /\nADD jacocoagent.jar /' /integcb/docker-containers/docker-autoscale/$folder/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../datalake/build/libs/datalake.jar to docker-datalake directory\033[0m\n"
cp -R ../docker-datalake/ ./integcb/docker-containers/docker-datalake/
cp ../datalake/build/libs/datalake.jar ./integcb/docker-containers/docker-datalake
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-datalake:/integcb/docker-containers/docker-datalake \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i '/datalake-\$VERSION/c\ADD datalake.jar /\nADD jacocoagent.jar /' /integcb/docker-containers/docker-datalake/$folder/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../freeipa/build/libs/freeipa.jar to docker-freeipa directory\033[0m\n"
cp -R ../docker-freeipa/ ./integcb/docker-containers/docker-freeipa/
cp ../freeipa/build/libs/freeipa.jar ./integcb/docker-containers/docker-freeipa
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-freeipa:/integcb/docker-containers/docker-freeipa \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i '/freeipa-\$VERSION/c\ADD freeipa.jar /\nADD jacocoagent.jar /' /integcb/docker-containers/docker-freeipa/$folder/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../environment/build/libs/environment.jar to docker-environment directory\033[0m\n"
cp -R ../docker-environment/ ./integcb/docker-containers/docker-environment/
cp ../environment/build/libs/environment.jar ./integcb/docker-containers/docker-environment
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-environment:/integcb/docker-containers/docker-environment \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i '/environment-\$VERSION/c\ADD environment.jar /\nADD jacocoagent.jar /' /integcb/docker-containers/docker-environment/$folder/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../environment/build/libs/remote-environment.jar to docker-environment-remote directory\033[0m\n"
cp -R ../docker-environment-remote/ ./integcb/docker-containers/docker-environment-remote/
cp ../environment-remote/build/libs/remote-environment.jar ./integcb/docker-containers/docker-environment-remote
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-environment-remote:/integcb/docker-containers/docker-environment-remote \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i '/environment-remote-\$VERSION/c\ADD remote-environment.jar /\nADD jacocoagent.jar /' /integcb/docker-containers/docker-environment-remote/$folder/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../redbeams/build/libs/redbeams.jar to docker-redbeams directory\033[0m\n"
cp -R  ../docker-redbeams/ ./integcb/docker-containers/docker-redbeams/
cp ../redbeams/build/libs/redbeams.jar ./integcb/docker-containers/docker-redbeams
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-redbeams:/integcb/docker-containers/docker-redbeams \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i '/redbeams-\$VERSION/c\ADD redbeams.jar /\nADD jacocoagent.jar /' /integcb/docker-containers/docker-redbeams/$folder/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../externalized-compute/build/libs/externalizedcompute.jar to docker-externalized-compute directory\033[0m\n"
cp -R ../docker-externalized-compute/ ./integcb/docker-containers/docker-externalized-compute/
cp ../externalized-compute/build/libs/externalizedcompute.jar ./integcb/docker-containers/docker-externalized-compute/externalized-compute.jar
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-externalized-compute:/integcb/docker-containers/docker-externalized-compute \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i '/externalized-compute-\$VERSION/c\ADD externalized-compute.jar /\nADD jacocoagent.jar /' /integcb/docker-containers/docker-externalized-compute/$folder/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../mock-infrastructure/build/libs/mock-infrastructure.jar to docker-mock-infrastructure directory\033[0m\n"
mkdir ./integcb/docker-containers/docker-mock-infrastructure/
cp ../mock-infrastructure/build/libs/mock-infrastructure.jar ./integcb/docker-containers/docker-mock-infrastructure
cp ../mock-infrastructure/deploy.sh ./integcb/docker-containers/docker-mock-infrastructure
cp ../mock-infrastructure/Dockerfile ./integcb/docker-containers/docker-mock-infrastructure
cp ../mock-infrastructure/Makefile ./integcb/docker-containers/docker-mock-infrastructure
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-mock-infrastructure:/integcb/docker-containers/docker-mock-infrastructure \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i '/mock-infrastructure-\$VERSION/c\ADD mock-infrastructure.jar /\nADD jacocoagent.jar /' /integcb/docker-containers/docker-mock-infrastructure/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../mock-thunderhead/build/libs/mock-thunderhead.jar to docker-mock-thunderhead directory\033[0m\n"
mkdir ./integcb/docker-containers/docker-mock-thunderhead/
cp ../mock-thunderhead/build/libs/mock-thunderhead.jar ./integcb/docker-containers/docker-mock-thunderhead
cp ../mock-thunderhead/deploy.sh ./integcb/docker-containers/docker-mock-thunderhead
cp ../mock-thunderhead/Dockerfile ./integcb/docker-containers/docker-mock-thunderhead
cp ../mock-thunderhead/Makefile ./integcb/docker-containers/docker-mock-thunderhead
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-mock-thunderhead:/integcb/docker-containers/docker-mock-thunderhead \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i '/mock-thunderhead-\$VERSION/c\ADD mock-thunderhead.jar /\nADD jacocoagent.jar /' /integcb/docker-containers/docker-mock-thunderhead/Dockerfile"

date
echo -e "\n\033[1;96m--- Download latest JaCoCo agent\033[0m\n"
mkdir -p ./integcb/jacoco
LATEST_JACOCO_VERSION=$(curl -s https://search.maven.org/solrsearch/select?q=g:org.jacoco+AND+a:org.jacoco.agent | jq -r '.response.docs[0].latestVersion')
curl -sS https://nexus-private.eng.cloudera.com/nexus/repository/public/org/jacoco/org.jacoco.agent/${LATEST_JACOCO_VERSION}/org.jacoco.agent-${LATEST_JACOCO_VERSION}-runtime.jar > ./integcb/jacoco/jacocoagent.jar

date
echo -e "\n\033[1;96m--- Copy JaCoCo agent JAR\033[0m\n"
cp ./integcb/jacoco/jacocoagent.jar ./integcb/docker-containers/docker-cloudbreak/
cp ./integcb/jacoco/jacocoagent.jar ./integcb/docker-containers/docker-autoscale/
cp ./integcb/jacoco/jacocoagent.jar ./integcb/docker-containers/docker-datalake/
cp ./integcb/jacoco/jacocoagent.jar ./integcb/docker-containers/docker-freeipa/
cp ./integcb/jacoco/jacocoagent.jar ./integcb/docker-containers/docker-environment/
cp ./integcb/jacoco/jacocoagent.jar ./integcb/docker-containers/docker-environment-remote/
cp ./integcb/jacoco/jacocoagent.jar ./integcb/docker-containers/docker-redbeams/
cp ./integcb/jacoco/jacocoagent.jar ./integcb/docker-containers/docker-externalized-compute/
cp ./integcb/jacoco/jacocoagent.jar ./integcb/docker-containers/docker-mock-infrastructure/
cp ./integcb/jacoco/jacocoagent.jar ./integcb/docker-containers/docker-mock-thunderhead/

date
echo -e "\n\033[1;96m--- Change starter scripts to inlcude JaCoCo agent \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-cloudbreak:/integcb/docker-containers/docker-cloudbreak \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i 's~JAVA_OPTS} \${SECURITY_OPTS}~& -javaagent:/jacocoagent.jar=destfile=/jacoco.exec,output=file ~' /integcb/docker-containers/docker-cloudbreak/bootstrap/start_cloudbreak_app.sh"
docker run -v "$(pwd)"/integcb/docker-containers/docker-datalake:/integcb/docker-containers/docker-datalake \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i 's~JAVA_OPTS} \${SECURITY_OPTS}~& -javaagent:/jacocoagent.jar=destfile=/jacoco.exec,output=file ~' /integcb/docker-containers/docker-datalake/bootstrap/start_datalake_app.sh"
docker run -v "$(pwd)"/integcb/docker-containers/docker-autoscale:/integcb/docker-containers/docker-autoscale \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i 's~JAVA_OPTS} \${SECURITY_OPTS}~& -javaagent:/jacocoagent.jar=destfile=/jacoco.exec,output=file ~' /integcb/docker-containers/docker-autoscale/bootstrap/start_autoscale_app.sh"
docker run -v "$(pwd)"/integcb/docker-containers/docker-freeipa:/integcb/docker-containers/docker-freeipa \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i 's~JAVA_OPTS} \${SECURITY_OPTS}~& -javaagent:/jacocoagent.jar=destfile=/jacoco.exec,output=file ~' /integcb/docker-containers/docker-freeipa/bootstrap/start_freeipa_app.sh"
docker run -v "$(pwd)"/integcb/docker-containers/docker-environment:/integcb/docker-containers/docker-environment \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i 's~JAVA_OPTS} \${SECURITY_OPTS}~& -javaagent:/jacocoagent.jar=destfile=/jacoco.exec,output=file ~' /integcb/docker-containers/docker-environment/bootstrap/start_environment_app.sh"
docker run -v "$(pwd)"/integcb/docker-containers/docker-redbeams:/integcb/docker-containers/docker-redbeams \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i 's~JAVA_OPTS} \${SECURITY_OPTS}~& -javaagent:/jacocoagent.jar=destfile=/jacoco.exec,output=file ~' /integcb/docker-containers/docker-redbeams/bootstrap/start_redbeams_app.sh"
docker run -v "$(pwd)"/integcb/docker-containers/docker-externalized-compute:/integcb/docker-containers/docker-externalized-compute \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i 's~JAVA_OPTS} \${SECURITY_OPTS}~& -javaagent:/jacocoagent.jar=destfile=/jacoco.exec,output=file ~' /integcb/docker-containers/docker-externalized-compute/bootstrap/start_externalizedcompute_app.sh"
docker run -v "$(pwd)"/integcb/docker-containers/docker-mock-infrastructure:/integcb/docker-containers/docker-mock-infrastructure \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i 's~CMD (java~& -javaagent:/jacocoagent.jar=destfile=/jacoco.exec,output=file~' /integcb/docker-containers/docker-mock-infrastructure/Dockerfile"
docker run -v "$(pwd)"/integcb/docker-containers/docker-mock-thunderhead:/integcb/docker-containers/docker-mock-thunderhead \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i 's~CMD (java~& -javaagent:/jacocoagent.jar=destfile=/jacoco.exec,output=file~' /integcb/docker-containers/docker-mock-thunderhead/Dockerfile"
docker run -v "$(pwd)"/integcb/docker-containers/docker-environment-remote:/integcb/docker-containers/docker-environment-remote \
 docker-private.infra.cloudera.com/cloudera_thirdparty/busybox:1.31.1 /bin/sh -c "sed -i 's~JAVA_OPTS} \${SECURITY_OPTS}~& -javaagent:/jacocoagent.jar=destfile=/jacoco.exec,output=file ~' /integcb/docker-containers/docker-environment-remote/bootstrap/start_remoteenvironment_app.sh"

echo -e "\n\033[1;96m--- Build docker images\033[0m\n"
  docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak:dev ./integcb/docker-containers/docker-cloudbreak -f ./integcb/docker-containers/docker-cloudbreak/$folder/Dockerfile $special_build_args & \
  docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-datalake:dev ./integcb/docker-containers/docker-datalake -f ./integcb/docker-containers/docker-datalake/$folder/Dockerfile $special_build_args & \
  docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-autoscale:dev ./integcb/docker-containers/docker-autoscale -f ./integcb/docker-containers/docker-autoscale/$folder/Dockerfile $special_build_args & \
  docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-freeipa:dev ./integcb/docker-containers/docker-freeipa -f ./integcb/docker-containers/docker-freeipa/$folder/Dockerfile $special_build_args & \
  docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-environment:dev ./integcb/docker-containers/docker-environment -f ./integcb/docker-containers/docker-environment/$folder/Dockerfile $special_build_args & \
  docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-redbeams:dev ./integcb/docker-containers/docker-redbeams -f ./integcb/docker-containers/docker-redbeams/$folder/Dockerfile $special_build_args & \
  docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-externalizedcompute:dev ./integcb/docker-containers/docker-externalized-compute -f ./integcb/docker-containers/docker-externalized-compute/$folder/Dockerfile $special_build_args & \
  docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-mock-infrastructure:dev ./integcb/docker-containers/docker-mock-infrastructure & \
  docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-mock-thunderhead:dev ./integcb/docker-containers/docker-mock-thunderhead & \
  docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-remote-environment:dev ./integcb/docker-containers/docker-environment-remote -f ./integcb/docker-containers/docker-environment-remote/$folder/Dockerfile $special_build_args
wait
