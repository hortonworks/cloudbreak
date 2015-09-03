git checkout master
git fetch
git reset --hard origin/master

export VER=$(cat VERSION)
export REL_DATE="[v${VER}] - $(date +%Y-%m-%d)"

CHANGE_FILE=docs/changelog.md
# edit changelog
sed -i "s/## Unreleased/## $REL_DATE/" CHANGELOG.md
printf '## Unreleased\n\n### Fixed\n\n### Added\n\n### Removed\n\n### Changed\n'| cat - $CHANGE_FILE | tee $CHANGE_FILE


git checkout -b release-${VER} origin/master
git push origin release-$VER
hub pull-request -b release -m "release $VER"
git checkout master


export NEW_VER="${VER%.*}.$((${VER##*.}+1))"
echo $NEW_VER > VERSION
git commit -m "Update version to $NEW_VER" VERSION $CHANGE_FILE
git push origin master
