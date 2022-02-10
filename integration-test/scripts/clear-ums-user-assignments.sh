#!/usr/bin/env bash
set -ex

: ${USERS_FILE:="./src/main/resources/ums-users/api-credentials.json"}
: ${DEFAULT_USER_PATH_IN_JSON:=".dev.default"}

install_requirements() {
  echo "install jq"
  wget -O ~/jq https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64
  chmod +x ~/jq
  cp ~/jq /usr/bin

  echo "install virtualenv"
  pip install virtualenv

  echo "install cdp-cli"
  mkdir ~/cdpclienv
  virtualenv ~/cdpclienv
  source ~/cdpclienv/bin/activate
  ~/cdpclienv/bin/pip install cdpcli
  ~/cdpclienv/bin/pip install --upgrade cdpcli
}

get_admin_access() {
  ACCESS_KEY=$(jq "[${DEFAULT_USER_PATH_IN_JSON}[] | select(.admin == true)][0].accessKey " -r $USERS_FILE)
  SECRET_KEY=$(jq "[${DEFAULT_USER_PATH_IN_JSON}[] | select(.admin == true)][0].secretKey " -r $USERS_FILE)
  ~/cdpclienv/bin/cdp configure set cdp_access_key_id "$ACCESS_KEY" --profile ums_cleanup
  ~/cdpclienv/bin/cdp configure set cdp_private_key "$SECRET_KEY" --profile ums_cleanup
  ~/cdpclienv/bin/cdp configure set cdp_endpoint_url "https://api.dps.mow-dev.cloudera.com/" --profile ums_cleanup
  ~/cdpclienv/bin/cdp configure set endpoint_url "https://%sapi.thunderhead-dev.cloudera.com/" --profile ums_cleanup
}

cleanup_users() {
  jq "${DEFAULT_USER_PATH_IN_JSON}[].crn" -r $USERS_FILE | while read userCrn; do
    echo "Clean up resource role assignments of $userCrn, if there is any."
    cleanup_user $userCrn
  done
}

cleanup_user() {
  RESOURCE_ASSIGNMENTS_JSON=$(get_resource_assignments "$1")
  cleanup_assignments "$1" "$RESOURCE_ASSIGNMENTS_JSON"
  if [[ $(echo "$RESOURCE_ASSIGNMENTS_JSON" | jq ".nextToken") != null ]]; then
    cleanup_user "$1"
  fi
}

get_resource_assignments() {
  if [[ "$1" == *"machineUser"* ]]; then
    ~/cdpclienv/bin/cdp iam list-machine-user-assigned-resource-roles --machine-user "$1" --profile ums_cleanup
  else
    ~/cdpclienv/bin/cdp iam list-user-assigned-resource-roles --user "$1" --profile ums_cleanup
  fi
}

cleanup_assignments() {
  userCrn="$1"
  echo "$2" | jq -r ".resourceAssignments[]| [.resourceCrn, .resourceRoleCrn] | @tsv" |
    while IFS=$'\t' read -r resourceCrn resourceRoleCrn; do
      echo "Unassigning $resourceRoleCrn from user $userCrn regarding resource $resourceCrn."
      if [[ "$userCrn" == *"machineUser"* ]]; then
        ~/cdpclienv/bin/cdp iam unassign-machine-user-resource-role --machine-user "$userCrn" --resource-crn "$resourceCrn" --resource-role-crn "$resourceRoleCrn" --profile ums_cleanup
      else
        ~/cdpclienv/bin/cdp iam unassign-user-resource-role --user "$userCrn" --resource-crn "$resourceCrn" --resource-role-crn "$resourceRoleCrn" --profile ums_cleanup
      fi
    done
}

main() {
  if [[ -f "$USERS_FILE" ]]; then
    install_requirements
    get_admin_access
    cleanup_users
  else
    echo "$USERS_FILE does not exist, please fetch them first."
    exit 1
  fi
}

main "$@"
