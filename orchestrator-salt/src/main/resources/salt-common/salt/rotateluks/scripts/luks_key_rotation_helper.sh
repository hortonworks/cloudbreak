#!/bin/bash

set -ex -o pipefail

export LUKS_VOLUME_NAME="cdp-luks"
export LUKS_DIR="/etc/$LUKS_VOLUME_NAME"
export LUKS_BACKING_FILE="$LUKS_DIR/$LUKS_VOLUME_NAME"
export PASSPHRASE_TMPFS="/mnt/cdp-luks_passphrase_tmpfs"
export PASSPHRASE_TMPFS_SIZE="16k"
export PASSPHRASE_PLAINTEXT="$PASSPHRASE_TMPFS/passphrase"
export PASSPHRASE_CIPHERTEXT="$LUKS_DIR/passphrase_ciphertext"
export ENCRYPTION_KEY_FILE="$LUKS_DIR/passphrase_encryption_key"
export LUKS_LOG_DIR="/var/log/$LUKS_VOLUME_NAME"
export AWS_USE_FIPS_ENDPOINT=true
export AWS_RETRY_MODE=standard
export AWS_MAX_ATTEMPTS=15

find_loop_device() {
  local loop_device_count
  loop_device_count="$(losetup --associated "$LUKS_BACKING_FILE" | wc -l | tr -d '\n')"
  if [[ $loop_device_count == 1 ]]; then
    LOOP_DEVICE="$(losetup --associated "$LUKS_BACKING_FILE" | awk '{print $1}' | tr -d ':')"
  elif [[ $loop_device_count -lt 1 ]]; then
    # Associate a loop device with the LUKS backing file
    LOOP_DEVICE=$(losetup --find --show "$LUKS_BACKING_FILE")
  else
    echo "There are multiple loop devices associated with the LUKS backing file. Maunally detach all of them but one and try again."
    exit 1
  fi
}

setup_tmpfs_for_plaintext_passphrase() {
  if ! mountpoint "$PASSPHRASE_TMPFS"; then
    mount -t tmpfs -o size="$PASSPHRASE_TMPFS_SIZE",mode=700 tmpfs "$PASSPHRASE_TMPFS"
  else
    echo "Plaintext passphrase tmpfs already mounted."
  fi
}

clean_up_keyslots() {
  local keyslot_count
  keyslot_count="$(cryptsetup luksDump "$LOOP_DEVICE" | grep -cE '^\s*[0-9]+:\s+luks2')"
  if [[ "$keyslot_count" -gt 1 ]]; then
    local does_open
    local does_not_open
    decrypt_ciphertext "$PASSPHRASE_CIPHERTEXT" "$1"
    for ((i=0; i <= keyslot_count-1; i++)); do
      if cryptsetup open "$LOOP_DEVICE" --key-file "$PASSPHRASE_PLAINTEXT" --type luks2 --test-passphrase --key-slot "$i"; then
        does_open+=("$i")
      else
        does_not_open+=("$i")
      fi
    done
    echo "The current $PASSPHRASE_CIPHERTEXT opens keyslot(s): ${does_open[*]}"
    echo "The current $PASSPHRASE_CIPHERTEXT does NOT open keyslot(s): ${does_not_open[*]}"
    if [[ "${#does_open[@]}" -ge 1 ]]; then
      local slots_to_kill
      slots_to_kill=("${does_not_open[@]}" "${does_open[@]:1}")
      echo "At least one keyslot can be opened with the current $PASSPHRASE_CIPHERTEXT. Killing slots which cannot opened and duplicate slots which can: ${slots_to_kill[*]}"
      for slot in "${slots_to_kill[@]}"; do
        cryptsetup luksKillSlot "$LOOP_DEVICE" "$slot" --type luks2 --key-file "$PASSPHRASE_PLAINTEXT"
      done
    else
      echo "No keyslot can be opened with the current $PASSPHRASE_CIPHERTEXT. Please check manually, if there is a backup of the correct ciphertext!"
      exit 2
    fi
  fi
}

decrypt_ciphertext() {
  INSTANCE_ID="$(TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 10") && \
                         curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/instance-id)"
  METADATA_LOG_FILE="$LUKS_LOG_DIR/passphrase_decryption_md-$(date +"%F-%T")-rotation-$2.json"
  /usr/local/bin/aws-encryption-cli \
    --decrypt \
    --input "$1" \
    --output "$PASSPHRASE_PLAINTEXT" \
    --wrapping-keys provider=aws-kms key="$(cat "$ENCRYPTION_KEY_FILE")" \
    --metadata-output "$METADATA_LOG_FILE" \
    --encryption-context INSTANCE_ID="$INSTANCE_ID"
  chmod 600 "$PASSPHRASE_PLAINTEXT"
  chmod 600 "$METADATA_LOG_FILE"
}

remove_backup_files() {
  rm -r "$PASSPHRASE_PLAINTEXT.bak"
  rm -r "$PASSPHRASE_CIPHERTEXT.bak"
}
