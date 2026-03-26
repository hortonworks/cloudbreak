#!/usr/bin/env bash

: ${INTEGCB_LOCATION?"integcb location"}

if [[ -z "${INTEGRATIONTEST_YARN_QUEUE}" ]] && [[ "$AWS" != true ]]; then
  echo -e "\n\033[1;96m--- Stop prometheus container\033[0m\n"
  docker compose stop;
  tar -czvf prometheus-data.tar.gz prometheus-data/;
fi

echo -e "\n\033[1;96m--- Stop cbd containers\033[0m\n"
cd $INTEGCB_LOCATION;
docker compose stop;

echo -e "\n\033[1;96m--- Save Cluster Proxy logs to file\033[0m\n"
docker logs cbreak_jumpgate-interop_1 &> ../jumpgate-interop.log;
docker logs cbreak_jumpgate-admin_1 &> ../jumpgate-admin.log;
docker logs cbreak_jumpgate-proxy_1 &> ../jumpgate-proxy.log;

echo -e "\n\033[1;96m--- Save gateway and thunderhead-mock logs\033[0m\n"
docker logs cbreak_dev-gateway_1 &> ../dev-gateway.log;
docker logs cbreak_core-gateway_1 &> ../core-gateway.log;
docker logs cbreak_thunderhead-mock_1 &> ../thunderhead-mock.log;
docker logs cbreak_envoy_1 &> ../envoy.log;