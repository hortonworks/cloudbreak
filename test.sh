NEW_IMAGE=$(curl https://release.infra.cloudera.com/hwre-api/latestimage\?image_name\=ubi8/cldr-openjdk-11-runtime |jq .image_name --raw-output)
echo "The latest image from RE is $NEW_IMAGE"

NEW_IMAGE_LINE="FROM $NEW_IMAGE"
FROM_REGEX="FROM docker-private.*"

for i in $(find . -name "Dockerfile"); do
    sed -i "s|$FROM_REGEX|$NEW_IMAGE_LINE|g" $i
done

if [[ `git status --porcelain` ]]; then
  TODAY=$(date +'%m-%d-%Y')
  BRANCH_NAME=base-image-$TODAY
  git checkout -b $BRANCH_NAME
  git add .
  git commit -m "[JENKINS-AUTOMATION] Update Base image for $NEW_IMAGE"
  git push origin $BRANCH_NAME
  git request-pull $BRANCH_NAME https://github.com/hortonworks/cloudbreak.git master
else
  echo "No new base image from RE DB"
fi