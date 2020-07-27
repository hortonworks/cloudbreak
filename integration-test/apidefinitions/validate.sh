#!/bin/bash

function check() {
  for f in /json/$1.json ; do
    [ -f "$f" ] || continue
    echo check to ${f}
    swagger validate /json/${1}.json
    status=$?
    [ $status -eq 0 ] && echo "$f validation was successfull" || cstatus=1
  done;
}

cstatus=0
DataList="cloudbreak,freeipa,environment,datalake,redbeams,autoscale"
Field_Separator=$IFS

echo Cloudbreak swagger compatibility check
IFS=,
for val in $DataList; do
  check $val
done
IFS=$Field_Separator


[ $cstatus -eq 0 ] && echo "swagger validation finished succesfully" || echo "swagger validation failed"
exit $cstatus


