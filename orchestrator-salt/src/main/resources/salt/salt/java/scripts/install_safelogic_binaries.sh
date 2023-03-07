#!/usr/bin/env bash

set -e

: ${JRE_EXT_PATH?' JRE_EXT_PATH environment variable is required'}
: ${PAYWALL_AUTH?' PAYWALL_AUTH environment variable is required'}
: ${CCJ_PATH?' CCJ_PATH environment variable is required'}
: ${CCJ_HASH_PATH?' CCJ_HASH_PATH environment variable is required'}
: ${BCTLS_PATH?' BCTLS_PATH environment variable is required'}
: ${BCTLS_HASH_PATH?' BCTLS_HASH_PATH environment variable is required'}

log() {
  MESSAGE=$1
  echo "$(date '+%d/%m/%Y %H:%M:%S') - $MESSAGE "
}

install() {
  FILENAME=$1
  URL=$2
  HASH_URL=$3

  FILEPATH="${JRE_EXT_PATH}/${FILENAME}"
  DOWNLOAD_FILENAME=${URL##*/}
  HASH_FILENAME=${HASH_URL##*/}

  if [ -f $FILEPATH  ]; then
    log "SafeLogic binary $FILEPATH already installed"
    return
  else
    log "Downloading file $DOWNLOAD_FILENAME from $URL"
    curl -L -O -R --fail -u $PAYWALL_AUTH $URL

    log "Downloading hash $HASH_FILENAME from $HASH_URL"
    curl -L -O -R --fail -u $PAYWALL_AUTH $HASH_URL

    log "Verifying hash of $DOWNLOAD_FILENAME with hash '$(cat $HASH_FILENAME)'"
    sha256sum --quiet -c $HASH_FILENAME

    log "Removing hash file $HASH_FILENAME as it is no longer needed"
    rm $HASH_FILENAME

    PERMISSION=644
    log "Setting permissions of file $DOWNLOAD_FILENAME to $PERMISSION"
    chmod $PERMISSION $DOWNLOAD_FILENAME

    log "Moving $DOWNLOAD_FILENAME to $FILEPATH"
    mv $DOWNLOAD_FILENAME $FILEPATH

    log "Successfully installed SafeLogic binary $FILENAME"
  fi
}

log "Installing SafeLogic binaries to $JRE_EXT_PATH"

install "ccj.jar" "$CCJ_PATH" "$CCJ_HASH_PATH"
install "bctls.jar" "$BCTLS_PATH" "$BCTLS_HASH_PATH"

log "SafeLogic binaries successfully installed"
