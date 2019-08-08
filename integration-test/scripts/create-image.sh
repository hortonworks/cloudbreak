#!/usr/bin/env bash

set -e

docker login --username=$DOCKERHUB_USERNAME --password=$DOCKERHUB_PASSWORD
rm -rf ./integcb/docker-containers
mkdir -p ./integcb/docker-containers

echo -e "\n\033[1;96m--- Copy ../core/build/libs/cloudbreak.jar to docker-cloudbreak directory\033[0m\n"
if [[ "$VERSION" ]]; then
docker pull docker-private.infra.cloudera.com/cloudera/cloudbreak:$VERSION
docker tag docker-private.infra.cloudera.com/cloudera/cloudbreak:$VERSION docker-private.infra.cloudera.com/cloudera/cloudbreak:dev
else
cp -R ../docker-cloudbreak/ ./integcb/docker-containers/docker-cloudbreak/
cp ../core/build/libs/cloudbreak.jar ./integcb/docker-containers/docker-cloudbreak
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -d -v "$(pwd)"/integcb/docker-containers/docker-cloudbreak:/integcb/docker-containers/docker-cloudbreak \
 busybox /bin/sh -c "sed -i '/cloudbreak-\$VERSION/c\ADD cloudbreak.jar /' /integcb/docker-containers/docker-cloudbreak/Dockerfile"
echo -e "\n\033[1;96m--- Build dev cloudbreak test image\033[0m\n"
docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak:dev ./integcb/docker-containers/docker-cloudbreak
fi

echo -e "\n\033[1;96m--- Copy ../autoscale/build/libs/periscope.jar to docker-autoscale directory\033[0m\n"
if [[ "$VERSION" ]]; then
docker pull docker-private.infra.cloudera.com/cloudera/cloudbreak-autoscale:$VERSION
docker tag docker-private.infra.cloudera.com/cloudera/cloudbreak-autoscale:$VERSION docker-private.infra.cloudera.com/cloudera/cloudbreak-autoscale:dev
else
cp -R ../docker-autoscale/ ./integcb/docker-containers/docker-autoscale/
cp ../autoscale/build/libs/periscope.jar ./integcb/docker-containers/docker-autoscale
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -d -v "$(pwd)"/integcb/docker-containers/docker-autoscale:/integcb/docker-containers/docker-autoscale \
 busybox /bin/sh -c "sed -i '/periscope-\$VERSION/c\ADD periscope.jar /' /integcb/docker-containers/docker-autoscale/Dockerfile"
echo -e "\n\033[1;96m--- Build dev autoscale test image\033[0m\n"
docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-autoscale:dev ./integcb/docker-containers/docker-autoscale
fi

echo -e "\n\033[1;96m--- Copy ../datalake/build/libs/datalake.jar to docker-datalake directory\033[0m\n"
if [[ "$VERSION" ]]; then
docker pull docker-private.infra.cloudera.com/cloudera/cloudbreak-datalake:$VERSION
docker tag docker-private.infra.cloudera.com/cloudera/cloudbreak-datalake:$VERSION docker-private.infra.cloudera.com/cloudera/cloudbreak-datalake:dev
else
cp -R ../docker-datalake/ ./integcb/docker-containers/docker-datalake/
cp ../datalake/build/libs/datalake.jar ./integcb/docker-containers/docker-datalake
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -d -v "$(pwd)"/integcb/docker-containers/docker-datalake:/integcb/docker-containers/docker-datalake \
 busybox /bin/sh -c "sed -i '/datalake-\$VERSION/c\ADD datalake.jar /' /integcb/docker-containers/docker-datalake/Dockerfile"
echo -e "\n\033[1;96m--- Build dev datalake test image\033[0m\n"
docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-datalake:dev ./integcb/docker-containers/docker-datalake
fi

echo -e "\n\033[1;96m--- Copy ../freeipa/build/libs/freeipa.jar to docker-freeipa directory\033[0m\n"
if [[ "$VERSION" ]]; then
docker pull docker-private.infra.cloudera.com/cloudera/cloudbreak-freeipa:$VERSION
docker tag docker-private.infra.cloudera.com/cloudera/cloudbreak-freeipa:$VERSION docker-private.infra.cloudera.com/cloudera/cloudbreak-freeipa:dev
else
cp -R ../docker-freeipa/ ./integcb/docker-containers/docker-freeipa/
cp ../freeipa/build/libs/freeipa.jar ./integcb/docker-containers/docker-freeipa
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -d -v "$(pwd)"/integcb/docker-containers/docker-freeipa:/integcb/docker-containers/docker-freeipa \
 busybox /bin/sh -c "sed -i '/freeipa-\$VERSION/c\ADD freeipa.jar /' /integcb/docker-containers/docker-freeipa/Dockerfile"
echo -e "\n\033[1;96m--- Build dev freeipa test image\033[0m\n"
docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-freeipa:dev ./integcb/docker-containers/docker-freeipa
fi

echo -e "\n\033[1;96m--- Copy ../environment/build/libs/environment.jar to docker-environment directory\033[0m\n"
if [[ "$VERSION" ]]; then
docker pull docker-private.infra.cloudera.com/cloudera/cloudbreak-environment:$VERSION
docker tag docker-private.infra.cloudera.com/cloudera/cloudbreak-environment:$VERSION docker-private.infra.cloudera.com/cloudera/cloudbreak-environment:dev
else
cp -R ../docker-environment/ ./integcb/docker-containers/docker-environment/
cp ../environment/build/libs/environment.jar ./integcb/docker-containers/docker-environment
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -d -v "$(pwd)"/integcb/docker-containers/docker-environment:/integcb/docker-containers/docker-environment \
 busybox /bin/sh -c "sed -i '/environment-\$VERSION/c\ADD environment.jar /' /integcb/docker-containers/docker-environment/Dockerfile"
echo -e "\n\033[1;96m--- Build dev environment test image\033[0m\n"
docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-environment:dev ./integcb/docker-containers/docker-environment
fi

echo -e "\n\033[1;96m--- Copy ../redbeams/build/libs/redbeams.jar to docker-redbeams directory\033[0m\n"
if [[ "$VERSION" ]]; then
docker pull docker-private.infra.cloudera.com/cloudera/cloudbreak-redbeams:$VERSION
docker tag docker-private.infra.cloudera.com/cloudera/cloudbreak-redbeams:$VERSION docker-private.infra.cloudera.com/cloudera/cloudbreak-redbeams:dev
else
cp -R  ../docker-redbeams/ ./integcb/docker-containers/docker-redbeams/
cp ../redbeams/build/libs/redbeams.jar ./integcb/docker-containers/docker-redbeams
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -d -v "$(pwd)"/integcb/docker-containers/docker-redbeams:/integcb/docker-containers/docker-redbeams \
 busybox /bin/sh -c "sed -i '/redbeams-\$VERSION/c\ADD redbeams.jar /' /integcb/docker-containers/docker-redbeams/Dockerfile"
echo -e "\n\033[1;96m--- Build dev redbeams test image\033[0m\n"
docker build -t docker-private.infra.cloudera.com/cloudera/cloudbreak-redbeams:dev ./integcb/docker-containers/docker-redbeams
fi
