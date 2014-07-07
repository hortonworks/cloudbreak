: ${NODE_PREFIX=amb}
: ${MYDOMAIN:=mycorp.kom}
: ${IMAGE:="sequenceiq/ambari"}
: ${INSTANCE_ID:="0"}

# installs jq to process jsons easily
curl -o /usr/bin/jq http://stedolan.github.io/jq/download/linux64/jq
chmod +x /usr/bin/jq

# jq expression that selects the json entry of the current instance from the array returned by the metadata service
INSTANCE_SELECTOR='. | map(select(.instanceId == "'$INSTANCE_ID'"))'
# jq expression that selects all the other json entries
OTHER_INSTANCES_SELECTOR='. | map(select(.instanceId != "'$INSTANCE_ID'"))'

# metadata service returns '204: no-content' if metadata is not ready yet and '200: ok' if it's completed
# every other http status codes mean that something unexpected happened
METADATA_STATUS=204
MAX_RETRIES=60
RETRIES=0