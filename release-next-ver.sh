git checkout master
git fetch
git reset --hard origin/master

export OLD_VER=$(cat VERSION)
export VER="${OLD_VER%.*}.$((${OLD_VER##*.}+1))"
export REL_DATE="[v${VER}] - $(date +%Y-%m-%d)"
echo $VER > VERSION

# edit CHANGELOG.md
sed -i "s/## Unreleased/## $REL_DATE/" CHANGELOG.md
echo -e '## Unreleased\n\n### Fixed\n\n### Added\n\n### Removed\n\n### Changed\n'| cat - CHANGELOG.md | tee CHANGELOG.md

git commit -m "prepare for release $VER" VERSION CHANGELOG.md
git push origin master

checkout -b release-${VER} origin/master
git push origin release-$VER
hub pull-request -b release -m "release $VER"
