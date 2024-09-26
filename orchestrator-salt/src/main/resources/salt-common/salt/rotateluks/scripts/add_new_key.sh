#!/bin/bash

set -ex -o pipefail

source /etc/cdp-luks/bin/rotation/luks_key_rotation_helper.sh

backup_ciphertext() {
  cp "$PASSPHRASE_CIPHERTEXT" "$PASSPHRASE_CIPHERTEXT.bak"
}

backup_plaintext() {
  if [[ ! -e "$PASSPHRASE_PLAINTEXT" ]]; then
    decrypt_ciphertext "$PASSPHRASE_CIPHERTEXT" "rotate"
  fi
  cp "$PASSPHRASE_PLAINTEXT" "$PASSPHRASE_PLAINTEXT.bak"
}

overwrite_plaintext() {
  aws kms generate-random --number-of-bytes 64 --output text --query Plaintext | base64 --decode > "$PASSPHRASE_PLAINTEXT"
  chmod 600 "$PASSPHRASE_PLAINTEXT"
}

overwrite_ciphertext() {
  INSTANCE_ID="$(TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 10") && \
                                  curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/instance-id)"
  METADATA_LOG_FILE="$LUKS_LOG_DIR/passphrase_encryption_md-$(date +"%F-%T")-rotation-rotate.json"
  /usr/local/bin/aws-encryption-cli \
    --encrypt \
    --input "$PASSPHRASE_PLAINTEXT" \
    --output "$PASSPHRASE_CIPHERTEXT" \
    --wrapping-keys provider=aws-kms key="$(cat "$ENCRYPTION_KEY_FILE")" \
    --metadata-output "$METADATA_LOG_FILE" \
    --encryption-context INSTANCE_ID="$INSTANCE_ID"
  chmod 600 "$PASSPHRASE_CIPHERTEXT"
  chmod 600 "$METADATA_LOG_FILE"
}

add_new_passphrase_to_luks() {
  cryptsetup luksAddKey "$LOOP_DEVICE" "$PASSPHRASE_PLAINTEXT" \
    --key-file "$PASSPHRASE_PLAINTEXT.bak" \
    --type luks2 \
    --hash "sha3-512" \
    --iter-time 2000 \
    --pbkdf "pbkdf2" \
    --debug;
}

main () {
  find_loop_device
  setup_tmpfs_for_plaintext_passphrase
  clean_up_keyslots "rotate"
  backup_ciphertext
  backup_plaintext
  overwrite_plaintext
  overwrite_ciphertext
  add_new_passphrase_to_luks
}

main "$@"
