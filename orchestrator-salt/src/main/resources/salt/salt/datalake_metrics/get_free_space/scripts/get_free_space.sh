#!/bin/bash

set -o nounset
set -o pipefail

# Logging configuration
LOG_FILE=/var/log/get_free_space.log
doLog() {
  if [ -n "${1-}" ]; then
    echo "$(date "+%Y-%m-%dT%H:%M:%S") $1" >>$LOG_FILE
  fi
}

# Input validation
if [[ $# -lt 1 ]]; then
  doLog "Invalid inputs provided"
  doLog "Script requires the following input:"
  doLog "  1. Directory path to check free space for."
  echo '"freeSpace":-4'
  exit 1
fi

# Get directory path from command line argument
DIR="$1"

doLog "Checking free space for directory: $DIR"

# Check if directory exists
if [[ ! -d "$DIR" ]]; then
  doLog "ERROR: Directory '$DIR' does not exist"
  echo '"freeSpace":-1'
  exit 0
fi

# Check if directory is readable and writable
if [[ ! -r "$DIR" || ! -w "$DIR" ]]; then
  doLog "ERROR: Directory '$DIR' is not readable or writable"
  echo '"freeSpace":-2'
  exit 0
fi

# Get free space in KB
FREE_SPACE=$(df --block-size=1024 --output=avail "$DIR" 2>/dev/null | tail -1)

# Validate that we got a numeric value
if [[ ! "$FREE_SPACE" =~ ^[0-9]+$ ]]; then
  doLog "ERROR: Failed to get valid free space value for '$DIR'"
  echo '"freeSpace":-3'
  exit 0
fi

doLog "Free space for '$DIR': ${FREE_SPACE}KB"
echo "\"freeSpace\":$FREE_SPACE"
