#!/bin/bash

set -ex -o pipefail

source /etc/cdp-luks/bin/rotation/luks_key_rotation_helper.sh

main() {
  find_loop_device
  setup_tmpfs_for_plaintext_passphrase
  clean_up_keyslots "finalize"
  remove_backup_files
}

main "$@"
