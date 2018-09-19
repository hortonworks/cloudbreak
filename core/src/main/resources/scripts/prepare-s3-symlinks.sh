#!/bin/bash -e

function linkLatestVersion() {
  if [ -d "${1}" ] && [ ! -e "${3}" ]; then
    files=$(ls -1 ${1} | grep ${2} | sort -V)
    readarray -t fileArray <<< "$files"
    if [ ${#fileArray[@]} > 0 ]; then
      linkedFile=${fileArray[-1]}
      if [ ! -z "${linkedFile}" ]; then
        ln -s /usr//hdp/current/hadoop-client/lib/${linkedFile} ${3}
      fi
    fi
  fi
}

function setupAtlasServer() {
  if [ -d "/usr/hdp/current/atlas-server" ]; then
    if [ -e "/usr/hdp/current/hadoop-client/hadoop-aws.jar" ] && [ ! -e /usr/hdp/current/atlas-server/libext/hadoop-aws.jar ]; then
      ln -s /usr/hdp/current/hadoop-client/hadoop-aws.jar /usr/hdp/current/atlas-server/libext/hadoop-aws.jar
    fi
    linkLatestVersion /usr/hdp/current/hadoop-client/lib aws-java-sdk-core /usr/hdp/current/atlas-server/libext/aws-java-sdk-core.jar
    linkLatestVersion /usr/hdp/current/hadoop-client/lib aws-java-sdk-s3 /usr/hdp/current/atlas-server/libext/aws-java-sdk-s3.jar
    curl -iv -u $AMBARI_USER:$AMBARI_PASSWORD -H "X-Requested-By: ambari" -X POST -d '{"RequestInfo":{"command":"RESTART","context":"Restart all components required ATLAS","operation_level":{"level":"SERVICE","cluster_name":"$CLUSTER_NAME","service_name":"ATLAS"}},"Requests/resource_filters":[{"hosts_predicate":"HostRoles/stale_configs=false&HostRoles/cluster_name=$CLUSTER_NAME"}]}' http://$AMBARI_IP:8080/api/v1/clusters/$CLUSTER_NAME/requests
  fi
}

main() {
    setupAtlasServer
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"