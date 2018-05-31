#!/usr/bin/env bash

if [ -n "$1" ]
  then
    VDF_URL=$1
  else
    echo "Please specify a vdf-url"
    exit 1
fi

if [ -n "$2" ]
  then
    OS_FAMILY=$2
  else
    echo "Please specify an OS family type"
    exit 1
fi

echo "Using vdf url $VDF_URL"

wget $VDF_URL --output-document=vdf.xml

function getRepoBaseUrl()
{
    local repoName=$1
    local REPO_DATA=$(xmllint --xpath "//repository-version/repository-info/os/repo[reponame='$repoName']" vdf.xml)
    local BASE_URL=$(xmllint --xpath "//baseurl/text()" - <<<"$REPO_DATA")
    echo $BASE_URL
}

function generateDebianRepo()
{
    #define the template.
    local repoUrl=$1
    local repoName=$2
    cat << EOF
deb $repoUrl $repoName main
EOF
}

function generateYumRepo()
{
    #define the template.
    local repoUrl=$1
    local repoName=$2
    cat << EOF
[$repoName]
name=$repoName
baseurl=$repoUrl
path=/
enabled=1
gpgcheck=0
EOF
}

function generateZypperRepo()
{
    #define the template.
    local repoUrl=$1
    local repoName=$2
    cat << EOF
[$repoName]
name=$repoName
baseurl=$repoUrl
path=/
type=yast2
enabled=1
gpgcheck=0
EOF
}

function generateDebianRepoIfPresent(){
    local repoUrl=$1
    local repoName=$2
    if [ -n "$repoUrl" ]
      then
        generateDebianRepo $hdp_repo $repoName > "/etc/apt/sources.list.d/$repoName.list"
    fi
}

function generateYumRepoIfPresent(){
    local repoUrl=$1
    local repoName=$2
    if [ -n "$repoUrl" ]
      then
        generateYumRepo $hdp_repo $repoName > "/etc/yum.repos.d/$repoName.repo"
    fi
}

function generateZypperRepoIfPresent(){
    local repoUrl=$1
    local repoName=$2
    if [ -n "$repoUrl" ]
      then
        generateZypperRepo $hdp_repo $repoName > "/etc/zypp/repos.d/$repoName.repo"
    fi
}

hdp_repo=$(getRepoBaseUrl 'HDP')
hdf_repo=$(getRepoBaseUrl 'HDF')
hdp_utils_repo=$(getRepoBaseUrl 'HDP-UTILS')

case $OS_FAMILY in
  "Debian")
      generateDebianRepoIfPresent $hdp_repo 'HDP'
      generateDebianRepoIfPresent $hdf_repo 'HDF'
      generateDebianRepoIfPresent $hdp_utils_repo 'HDP-UTILS'
      apt-get update
  ;;
  "RedHat")
      generateYumRepoIfPresent $hdp_repo 'HDP'
      generateYumRepoIfPresent $hdf_repo 'HDF'
      generateYumRepoIfPresent $hdp_utils_repo 'HDP-UTILS'
  ;;
  "Suse")
      generateZypperRepoIfPresent $hdp_repo 'HDP'
      generateZypperRepoIfPresent $hdf_repo 'HDF'
      generateZypperRepoIfPresent $hdp_utils_repo 'HDP-UTILS'
      zypper ref
  ;;
esac


rm vdf.xml
echo "Removed vdf.xml file"
