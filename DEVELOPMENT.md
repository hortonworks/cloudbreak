## Contribution

Development process should happen on separate branches. Then a pull-request should be opened as usual.

To build the project
```
# make deps needed only once
make deps

make install
```

To run the unit tests:

```
make tests
```

If you want to test the binary CircleCI build from your branch named `fix-something`, to validate the PR binary `cbd` tool will be tested. It is built by CircleCI for each branch.

```
cbd update fix-something
```

## Testing

Shell scripts shouldn’t raise exceptions when it comes to unit testing. [basht](https://github.com/progrium/basht) is used for testing. See the reasoning: [why not bats or shunit2](https://github.com/progrium/basht#why-not-bats-or-shunit2)

Please cover your bash functions with unit tests and run test with:

```
make tests
```

## Release Process of the Clodbreak Deployer tool

The master branch is always built on [CircleCI](https://circleci.com/gh/sequenceiq/cloudbreak-deployer).
When you wan’t a new release, all you have to do:

- on the `master` branch:
  - make sure you change the `VERSION` file
  - update `CHANGELOG.md` with the release date
  - create a new **Unreleased** section in top of `CHANGELOG.md`
  
- create a PullRequest for the release branch:
  - create a new branch with a name like `release-0.5.x`
  - this branch should be the same as `origin/master`
  - create a pull request into `release` branch
  
Once the PR is merged, CircleCI will:
- create a new release on [GitHub releases tab](https://github.com/sequenceiq/cloudbreak-deployer/releases), with the help of [gh-release](https://github.com/progrium/gh-release).
- it will create the git tag with `v` prefix like: `v0.0.3`

Commands of the 0.0.3 release:

```
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
```

