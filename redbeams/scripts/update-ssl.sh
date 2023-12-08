#!/bin/bash -ex
curl -L https://github.com/mikefarah/yq/releases/download/2.4.1/yq_linux_amd64 -o yqtmp
chmod +x $(pwd)/yqtmp
curl -L https://github.com/jqlang/jq/releases/download/jq-1.6/jq-linux64 -o jqtmp
chmod +x $(pwd)/jqtmp

set +x
YQ_BINARY_PATH=$(pwd)/yqtmp \
JQ_BINARY_PATH=$(pwd)/jqtmp \
CB_AWS_ACCESS_KEY_ID=$CB_AWS_ACCESS_KEY_ID \
CB_AWS_SECRET_ACCESS_KEY=$CB_AWS_SECRET_ACCESS_KEY \
CB_AWS_GOV_ACCESS_KEY_ID=$CB_AWS_GOV_ACCESS_KEY_ID \
CB_AWS_GOV_SECRET_ACCESS_KEY=$CB_AWS_GOV_SECRET_ACCESS_KEY \
$(pwd)/scripts/import-aws-rds-root-certs.sh
set -x

rm $(pwd)/jqtmp
rm $(pwd)/yqtmp

if [[ -n `git status --porcelain` ]]; then
  TODAY=$(date +'%m-%d-%Y')
  BRANCH_NAME=cert-update-$TODAY
  git remote set-url origin https://$GH_TOKEN@github.infra.cloudera.com/cloudbreak/cloudbreak
  git branch -D $BRANCH_NAME || true
  git push origin --delete $BRANCH_NAME || true
  git checkout -b $BRANCH_NAME
  git status
  git add .
  git commit -m "[JENKINS-AUTOMATION] Update SSL certs for the latest certs"
  git push origin $BRANCH_NAME
  docker pull ghcr.io/supportpal/github-gh-cli
  docker run -e GH_TOKEN=$GH_TOKEN -e BRANCH_NAME=$BRANCH_NAME --entrypoint /bin/bash ghcr.io/supportpal/github-gh-cli -c 'echo $GH_TOKEN| gh auth login --hostname github.infra.cloudera.com --with-token && gh pr create --title "SSL cert update for the latest" --body "Automated Pull request to update the SSL certs" --base master --head $BRANCH_NAME --repo github.infra.cloudera.com/cloudbreak/cloudbreak'
else
  echo "No new ssl cert"
fi
