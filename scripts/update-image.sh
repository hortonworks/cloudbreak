NEW_IMAGE=$(curl https://release.infra.cloudera.com/hwre-api/latestimage\?image_name\=ubi8/cldr-openjdk-17-runtime-cis |jq .image_name --raw-output)
echo "The latest image from RE is $NEW_IMAGE"

cd ..
git checkout master
git reset --hard origin/master

NEW_IMAGE_LINE="FROM $NEW_IMAGE"
FROM_REGEX="FROM docker-private.* "

for i in $(find . -name "Dockerfile"); do
    sed -i "s|FROM docker-private.*|$NEW_IMAGE_LINE|g" $i
done

if [[ `git status --porcelain` ]]; then
  TODAY=$(date +'%m-%d-%Y')
  BRANCH_NAME=base-image-$TODAY
  git remote set-url origin https://$GH_TOKEN@github.infra.cloudera.com/cloudbreak/cloudbreak
  git branch -D $BRANCH_NAME | true
  git push origin --delete $BRANCH_NAME | true
  git checkout -b $BRANCH_NAME
  git status
  git add .
  git commit -m "[JENKINS-AUTOMATION] Update Base image for $NEW_IMAGE"
  git push origin $BRANCH_NAME
  docker pull ghcr.io/supportpal/github-gh-cli
  echo $GH_TOKEN | docker run -e GH_TOKEN=$GH_TOKEN -e BRANCH_NAME=$BRANCH_NAME --entrypoint /bin/bash ghcr.io/supportpal/github-gh-cli -c 'echo $GH_TOKEN| gh auth login --hostname github.infra.cloudera.com --with-token && gh pr create --title "Base image update for the latest RELENG image" --body "Automated Pull request to update the base image and reduce CVEs" --base master --head $BRANCH_NAME --repo github.infra.cloudera.com/cloudbreak/cloudbreak'
else
  echo "No new base image from RE DB"
fi