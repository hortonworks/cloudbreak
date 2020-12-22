#!/bin/bash

while read line; do
  echo "$line" | sed -u -E 's/(x-amz-security-token:)([A-Za-z0-9/+]*[=]*)/\1[FILTERED]/g'
done