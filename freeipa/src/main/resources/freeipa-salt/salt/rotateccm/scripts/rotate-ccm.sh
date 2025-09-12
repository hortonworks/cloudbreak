#!/bin/bash

source /usr/bin/ccmv2-helper.sh


check_agent_state() {
    echo "Probing for a running jumpgate-agent..."
    for attempt in {1..3}
    do
        if systemctl is-active -q jumpgate-agent
        then
          echo "jumpgate-agent is running."
          break
        elif [[ "$attempt" -eq 3 ]]
        then
            echo "Failed to activate jumpgate-agent service."
            return 1
        fi
        echo "Trying again in 2 sec..."
        sleep 2
    done
}

is_ccmv2_connectivity_accessible() {
    echo "Checking CCM V2 service endpoint connectivity..."
    for attempt in {1..3}
    do
        ccmstatus=$(cdp-doctor ccm status)
        echo $ccmstatus | grep -q ".v2."
        v2_found=$?
        echo $ccmstatus | grep -i "CCM is accessible" | grep -iq true
        v2_accessible=$?
        if [[ $v2_found -eq 0 && $v2_accessible -eq 0 ]]
        then
            echo "CCM V2 endpoint is accessible."
            break
        elif [[ "$attempt" -eq 3 ]]
        then
            echo "Connection to CCM V2 service is not available."
            return 1
        fi
        echo "Trying again in 2 sec..."
        sleep 2
    done
}

backup_ccmv2_config() {
  echo "Backup CCM V2 config.toml"

  CONFIG_FILE=/etc/jumpgate/config.toml
  if [ -f "${CONFIG_FILE}.backup" ]; then
    echo "Backup file already exist, skipping backup."
  elif [ -f "$CONFIG_FILE" ]; then
    cp -np "${CONFIG_FILE}" "${CONFIG_FILE}.backup"
    echo "config.toml.backup created"
  fi
}

main() {
  echo "Starting CCM V2 Jumpgate rotation at $(date +%Y-%m-%d\ %H:%M:%S)"

  backup_ccmv2_config

  IS_CCM_V2_JUMPGATE_ENABLED=true
  IS_FREEIPA=true
  INSTANCE_ID=
  if [[ "$CLOUD_PLATFORM" == "AWS" ]]; then
    INSTANCE_ID="$(TOKEN=`curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600"` && curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/instance-id)"
  elif [[ "$CLOUD_PLATFORM" == "AZURE" ]]; then
    INSTANCE_ID="`wget -q -O - --header="Metadata: true" 'http://169.254.169.254/metadata/instance/compute/name?api-version=2017-08-01&format=text'`"
  elif [[ "$CLOUD_PLATFORM" == "GCP" ]]; then
    INSTANCE_ID="`wget -q -O - --header="Metadata-Flavor: Google" 'http://metadata.google.internal/computeMetadata/v1/instance/name'`"
  fi

  echo "Instance ID: $INSTANCE_ID"

  setup_ccmv2

  echo "Setup CCMV2 finished"

  check_agent_state
  if [[ $? -ne 0 ]] ; then
    echo "Check agent state failed"
    exit 1
  fi

  echo "Restart jumpgate agent"

  systemctl restart jumpgate-agent

  echo "Finished at $(date +%Y-%m-%d\ %H:%M:%S)"
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
