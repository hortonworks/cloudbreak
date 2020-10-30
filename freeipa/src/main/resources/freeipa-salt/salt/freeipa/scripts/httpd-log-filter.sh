#!/bin/bash

while read line; do
  echo "$line" | sed -u -E 's/(cdpHashedPassword=|cdpUnencryptedKrbPrincipalKey=)([A-Za-z0-9{}/+]*)/\1[FILTERED]/g' >> /var/log/httpd/error_log
done