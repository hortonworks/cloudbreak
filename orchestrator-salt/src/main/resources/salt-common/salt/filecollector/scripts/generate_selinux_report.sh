#!/bin/bash

SELINUX_REPORT_LOG="/tmp/selinux_report.log"
SELINUX_DENIES_LOG="/tmp/selinux_denies.log"
SELINUX_AUDIT2ALLOW_LOG="/tmp/selinux_audit2allow.log"

log_to_report_file() {
    local message="$1"
    echo "$message" >> "$SELINUX_REPORT_LOG"
}

create_selinux_report() {
    # Clear previous report file
    > "$SELINUX_REPORT_LOG"

    log_to_report_file "======== Current SELinux mode ========"
    log_to_report_file "$(getenforce)"
    log_to_report_file
    log_to_report_file "======== SELinux config file contents ========"
    log_to_report_file "$(cat /etc/selinux/config)"
    log_to_report_file
    log_to_report_file "======== Installed policy modules ========"
    log_to_report_file "$(semodule -l)"
    log_to_report_file
    log_to_report_file "======== All defined types ========"
    log_to_report_file "$(seinfo -t)"
    log_to_report_file
    log_to_report_file "======== Permissive domain types ========"
    log_to_report_file "$(semanage permissive -l)"
    log_to_report_file
    log_to_report_file "======== File contexts ========"
    log_to_report_file "$(semanage fcontext -l)"
    log_to_report_file
    log_to_report_file "======== Port contexts ========"
    log_to_report_file "$(semanage port -l)"
    log_to_report_file
    log_to_report_file "======== User contexts ========"
    log_to_report_file "$(semanage user -l)"
    log_to_report_file
    log_to_report_file "======== SELinux Booleans ========"
    log_to_report_file "$(getsebool -a)"
    log_to_report_file
    log_to_report_file "======== Domains of current processes ========"
    log_to_report_file "$(ps -eZ)"
    log_to_report_file
}

collect_denies() {
    ausearch -m AVC,SELINUX_ERR,USER_SELINUX_ERR -i > "$SELINUX_DENIES_LOG"
    cat "$SELINUX_DENIES_LOG" | audit2allow > "$SELINUX_AUDIT2ALLOW_LOG"
}

main() {
    create_selinux_report
    collect_denies
}

main "$@"
