#!/bin/bash
set -x

CLOUD_PLATFORM="${cloudPlatform}"
START_LABEL=${platformDiskStartLabel}
PLATFORM_DISK_PREFIX=${platformDiskPrefix}
IS_GATEWAY=${gateway?c}
TMP_SSH_KEY="${tmpSshKey}"
PUBLIC_SSH_KEY="${publicSshKey}"
RELOCATE_DOCKER=${relocateDocker?c}

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log