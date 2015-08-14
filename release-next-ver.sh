git checkout master
git fetch
git reset --hard origin/master

export OLD_VER=$(cat VERSION)
export VER="${OLD_VER%.*}.$((${OLD_VER##*.}+1))"
export REL_DATE="[v${VER}] - $(date +%Y-%m-%d)"
echo $VER > VERSION

CHANGE_FILE=docs/changelog.md
# edit changelog 
sed -i "s/## Unreleased/## $REL_DATE/" CHANGELOG.md
printf '## Unreleased\n\n### Fixed\n\n### Added\n\n### Removed\n\n### Changed\n'| cat - $CHANGE_FILE | tee $CHANGE_FILE

git commit -m "prepare for release $VER" VERSION $CHANGE_FILE
git push origin master

git checkout -b release-${VER} origin/master
git push origin release-$VER
hub pull-request -b release -m "release $VER"
git checkout master
