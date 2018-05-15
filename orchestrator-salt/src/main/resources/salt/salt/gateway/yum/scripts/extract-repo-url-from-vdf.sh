#!/usr/bin/env bash

if [ -n "$1" ]
  then
    VDF_URL=$1
  else
    echo "Please specify a vdf-url"
    exit 1
fi

echo "Using vdf url $VDF_URL"

wget $VDF_URL --output-document=vdf.xml

#handle hdp repository
HDP_REPO_DATA=$(xmllint --xpath "//repository-version/repository-info/os/repo[reponame='HDP']" vdf.xml)
HDP_BASE_URL=$(xmllint --xpath "//baseurl/text()" - <<<"$HDP_REPO_DATA")
echo $HDP_BASE_URL >> "/tmp/hdp-repo-url.text"

#handle hdp-util repository
HDP_UTIL_REPO_DATA=$(xmllint --xpath "//repository-version/repository-info/os/repo[reponame='HDP-UTILS']" vdf.xml)
HDP_UTIL_BASE_URL=$(xmllint --xpath "//baseurl/text()" - <<<"$HDP_UTIL_REPO_DATA")
echo $HDP_UTIL_BASE_URL >> "/tmp/hdp-util-repo-url.text"

#cleanup hdp-utils.repo?
rm vdf.xml
echo "Removed vdf.xml file"
