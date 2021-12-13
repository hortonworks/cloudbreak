#!/bin/bash
# CLOUDERA SCRIPTS FOR LOG4J
#
# (C) Cloudera, Inc. 2021. All rights reserved.
#
# Applicable Open Source License: Apache License 2.0
#
# CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND. CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. CLOUDERA IS NOT LIABLE TO YOU,  AND WILL NOT DEFEND, INDEMNIFY, NOR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING FROM OR RELATED TO THE CODE. ND WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR ONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED TO, DAMAGES  RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF  BUSINESS ADVANTAGE OR UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
#
# --------------------------------------------------------------------------------------

set -e -o pipefail

# Program name
PROG=$(basename "$0")
BASEDIR=$(dirname "$0")
CDH_CDP_SCRIPT="$BASEDIR/cm_cdp_cdh_log4j_jndi_removal.sh"
HDP_SCRIPT="$BASEDIR/hdp_log4j_jndi_removal.sh"

log_info() {
    echo "INFO : ${1}" 1>&2
}

log_error() {
    error="$1"
    echo "ERROR: ${error}" 1>&2
}

subcommand_usage() {
    echo "Usage: $PROG (subcommand) [options]
    Subcommands:
        help              Prints this message
        cdh               Scan a CDH cluster node
        cdp               Scan a CDP cluster node
        hdp               Scan a HDP cluster node

    Options (cdh and cdp subcommands only):
        -t <targetdir>    Override target directory (default: distro-specific)
        -b <backupdir>    Override backup directory (default: /opt/cloudera/log4shell-backup)

    Environment Variables (cdh and cdp subcommands only):
        SKIP_JAR          If non-empty, skips scanning and patching .jar files
        SKIP_TGZ          If non-empty, skips scanning and patching .tar.gz files
        SKIP_HDFS         If non-empty, skips scanning and patching .tar.gz files in HDFS
        RUN_SCAN          If non-empty, runs a final scan for missed vulnerable files. This can take several hours.
" 1>&2
}


subcommand_cdh() {
    TARGETDIR=/opt/cloudera
    BACKUPDIR=/opt/cloudera/log4shell-backup

    unset OPTIND OPTARG options

    while getopts "t:b:" options
    do
        case ${options} in
            (t)
                TARGETDIR=${OPTARG}
                ;;
            (b)
                BACKUPDIR=${OPTARG}
                ;;
            (?)
                log_error "Invalid option ${OPTARG} passed .. "
                exit 1
                ;;
        esac
    done

    if [ ! -f $CDH_CDP_SCRIPT ]; then
      log_error "Could not find CDH/CDP script: $CDH_CDP_SCRIPT"
      exit 1
    fi

    log_info "Running CDH/CDP patcher script: $CDH_CDP_SCRIPT $TARGETDIR $BACKUPDIR"
    logfile=$(mktemp output_run_log4j_patcher.XXXXXX)
    log_info "Log file: $logfile"
    $CDH_CDP_SCRIPT "$TARGETDIR" "$BACKUPDIR" | tee "$logfile" 2>&1

    log_info "Finished"
}

subcommand_hdp() {
    log_info "Running HDP patcher script: $HDP_SCRIPT"
    logfile=$(mktemp output_run_log4j_patcher.XXXXXX)
    log_info "Log file: $logfile"
    $HDP_SCRIPT | tee "$logfile" 2>&1

    log_info "Finished"
}

main() {

    subcommand="$1"
    if [ x"${subcommand}x" == "xx" ]; then
        subcommand="help"
    else
        shift # past sub-command
    fi

    case $subcommand in
        help)
            subcommand_usage
            ;;
        cdh | cdp)
            subcommand_cdh "$@"
            ;;
        hdp)
            subcommand_hdp "$@"
            ;;
        *)
            # unknown option
            subcommand_usage
            exit 1
            ;;
    esac

    exit 0
}

main "$@"
exit 0
