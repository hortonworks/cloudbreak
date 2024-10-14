#!/bin/bash

set -ex -o pipefail

source /etc/cdp-luks/bin/rotation/luks_key_rotation_helper.sh

find_working_ciphertext() {
  local working_ciphertext_found=false
  local ciphertexts
  ciphertexts+=("$PASSPHRASE_CIPHERTEXT")
  if [[ -e "$PASSPHRASE_CIPHERTEXT.bak" ]]; then
    ciphertexts+=("$PASSPHRASE_CIPHERTEXT.bak")
  fi
  for ciphertext in "${ciphertexts[@]}"; do
    decrypt_ciphertext "$ciphertext" "rollback"
    if cryptsetup open "$LOOP_DEVICE" --key-file "$PASSPHRASE_PLAINTEXT" --type luks2 --test-passphrase; then
      if [[ "$ciphertext" != "$PASSPHRASE_CIPHERTEXT" ]]; then
        mv -f "$ciphertext" "$PASSPHRASE_CIPHERTEXT"
      fi
      working_ciphertext_found=true
      break
    fi
  done
  if [[ "$working_ciphertext_found" == false ]]; then
    echo "No working ciphertext found. Please check manually if there is a backup of the correct ciphertext!"
    exit 3
  fi
}

main() {
  find_loop_device
  setup_tmpfs_for_plaintext_passphrase
  find_working_ciphertext
  remove_backup_files
}

main "$@"
