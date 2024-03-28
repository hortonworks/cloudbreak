#!/usr/bin/env bash

: ${INTEGCB_LOCATION?"integcb location"}

if [[ -z "${INTEGRATIONTEST_YARN_QUEUE}" ]] && [[ "$AWS" != true ]]; then
  echo -e "\n\033[1;96m--- Stop prometheus container"
  docker compose --compatibility stop;
  tar -czvf prometheus-data.tar.gz prometheus-data/;
fi

echo -e "\n\033[1;96m--- Stop cbd containers"
cd $INTEGCB_LOCATION;
docker compose --compatibility stop;

echo -e "\n\033[1;96m--- Save Cluster Proxy log to cluster-proxy.log file"
docker logs cbreak_cluster-proxy_1 &> ../cluster-proxy.log;
docker logs cbreak_cluster-proxy-health-check-worker_1 &> ../cluster-proxy-health.log;

echo -e "\n\033[1;96m--- Save gateway and thunderhead-mock logs"
docker logs cbreak_dev-gateway_1 &> ../dev-gateway.log;
docker logs cbreak_core-gateway_1 &> ../core-gateway.log;
docker logs cbreak_thunderhead-mock_1 &> ../thunderhead-mock.log;