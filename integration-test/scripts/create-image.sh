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
 busybox:1.31.1 /bin/sh -c "sed -i '/cloudbreak-\$VERSION/c\ADD cloudbreak.jar /' /integcb/docker-containers/docker-cloudbreak/$folder/Dockerfile"
echo "StatusCode######## $?"

date
echo -e "\n\033[1;96m--- Copy ../autoscale/build/libs/periscope.jar to docker-autoscale directory\033[0m\n"
cp -R ../docker-autoscale/ ./integcb/docker-containers/docker-autoscale/
cp ../autoscale/build/libs/periscope.jar ./integcb/docker-containers/docker-autoscale
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-autoscale:/integcb/docker-containers/docker-autoscale \
 busybox:1.31.1 /bin/sh -c "sed -i '/periscope-\$VERSION/c\ADD periscope.jar /' /integcb/docker-containers/docker-autoscale/$folder/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../datalake/build/libs/datalake.jar to docker-datalake directory\033[0m\n"
cp -R ../docker-datalake/ ./integcb/docker-containers/docker-datalake/
cp ../datalake/build/libs/datalake.jar ./integcb/docker-containers/docker-datalake
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-datalake:/integcb/docker-containers/docker-datalake \
 busybox:1.31.1 /bin/sh -c "sed -i '/datalake-\$VERSION/c\ADD datalake.jar /' /integcb/docker-containers/docker-datalake/$folder/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../freeipa/build/libs/freeipa.jar to docker-freeipa directory\033[0m\n"
cp -R ../docker-freeipa/ ./integcb/docker-containers/docker-freeipa/
cp ../freeipa/build/libs/freeipa.jar ./integcb/docker-containers/docker-freeipa
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-freeipa:/integcb/docker-containers/docker-freeipa \
 busybox:1.31.1 /bin/sh -c "sed -i '/freeipa-\$VERSION/c\ADD freeipa.jar /' /integcb/docker-containers/docker-freeipa/$folder/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../environment/build/libs/environment.jar to docker-environment directory\033[0m\n"
cp -R ../docker-environment/ ./integcb/docker-containers/docker-environment/
cp ../environment/build/libs/environment.jar ./integcb/docker-containers/docker-environment
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-environment:/integcb/docker-containers/docker-environment \
 busybox:1.31.1 /bin/sh -c "sed -i '/environment-\$VERSION/c\ADD environment.jar /' /integcb/docker-containers/docker-environment/$folder/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../environment/build/libs/remote-environment.jar to docker-environment-remote directory\033[0m\n"
cp -R ../docker-environment-remote/ ./integcb/docker-containers/docker-environment-remote/
cp ../environment-remote/build/libs/remote-environment.jar ./integcb/docker-containers/docker-environment-remote
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-environment-remote:/integcb/docker-containers/docker-environment-remote \
 busybox:1.31.1 /bin/sh -c "sed -i '/environment-remote-\$VERSION/c\ADD remote-environment.jar /' /integcb/docker-containers/docker-environment-remote/$folder/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../redbeams/build/libs/redbeams.jar to docker-redbeams directory\033[0m\n"
cp -R  ../docker-redbeams/ ./integcb/docker-containers/docker-redbeams/
cp ../redbeams/build/libs/redbeams.jar ./integcb/docker-containers/docker-redbeams
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-redbeams:/integcb/docker-containers/docker-redbeams \
 busybox:1.31.1 /bin/sh -c "sed -i '/redbeams-\$VERSION/c\ADD redbeams.jar /' /integcb/docker-containers/docker-redbeams/$folder/Dockerfile"

date
echo -e "\n\033[1;96m--- Copy ../externalized-compute/build/libs/externalizedcompute.jar to docker-externalized-compute directory\033[0m\n"
cp -R ../docker-externalized-compute/ ./integcb/docker-containers/docker-externalized-compute/
cp ../externalized-compute/build/libs/externalizedcompute.jar ./integcb/docker-containers/docker-externalized-compute/externalized-compute.jar
date
echo -e "\n\033[1;96m--- Change Dockerfile \033[0m\n"
docker run -v "$(pwd)"/integcb/docker-containers/docker-externalized-compute:/integcb/docker-containers/docker-externalized-compute \
 busybox:1.31.1 /bin/sh -c "sed -i '/externalized-compute-\$VERSION/c\ADD externalized-compute.jar /' /integcb/docker-containers/docker-externalized-compute/$folder/Dockerfile"

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
 busybox:1.31.1 /bin/sh -c "sed -i '/mock-infrastructure-\$VERSION/c\ADD mock-infrastructure.jar /' /integcb/docker-containers/docker-mock-infrastructure/Dockerfile"

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
 busybox:1.31.1 /bin/sh -c "sed -i '/mock-thunderhead-\$VERSION/c\ADD mock-thunderhead.jar /' /integcb/docker-containers/docker-mock-thunderhead/Dockerfile"

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
