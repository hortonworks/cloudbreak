# Environments

##Cloudbreak Deployer

### Contributing

Install GO:
```
brew install go
```
Export GOPATH and add to PATH the go/bin directory:

Get cloudbreak-deployer project with GO:
```
go get github.com/hortonworks/cloudbreak-deployer
``` 

Development process happens on separate branches. Open a pull-request to contribute your changes.

To build the project:
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
branch=fix-something
circle_url=https://circleci.com/api/v1/project/hortonworks/cloudbreak-deployer
circle_token=[CIRCLE_TOKEN]
latest_build=$(curl -s -G -d circle-token=${circle_token} ${circle_url}/tree/${branch}?filter=completed\&limit=1 | grep -m 1 build_num | sed 's/[^0-9]*//g')
curl -sL $(curl -s -G -d circle-token=${circle_token} ${circle_url}/${latest_build}/artifacts | grep url | grep -i $(uname) | cut -d\" -f 4) | tar -xz -C $(dirname $(which cbd))
```

### Snapshots

We recommend that you always use the latest release, but you might also want to check new features or bugfixes in the `master` branch.
All successful builds from the `master` branch are uploaded to the public S3 bucket. You can download it using:

```
curl -L s3.amazonaws.com/public-repo-1.hortonworks.com/HDP/cloudbreak/cloudbreak-deployer_snapshot_$(uname)_x86_64.tgz | tar -xz
```

Instead of overwriting the released version, download it to a **local directory** and use it by referring to it as `./cbd`

```
./cbd --version
```

### Testing

Shell scripts shouldn’t raise exceptions when it comes to unit testing. [basht](https://github.com/progrium/basht) is
 used for testing. See [this link](https://github.com/progrium/basht#why-not-bats-or-shunit2) to learn why not bats or shunit2.

You must cover your bash functions with unit tests and run the test with:

```
make tests
```

### Release Process for the Clodbreak Deployer Tool

The master branch is always built on [CircleCI](https://circleci.com/gh/hortonworks/cloudbreak-deployer).

When you want to create a new release, run:

```
make release-next-ver
```

The `make release-next-ver` performs the following steps:

 * On the `master` branch:
    * Updates the `VERSION` file by increasing the **patch** version number (For example, from 0.5.2 to 0.5.3).
    * Updates `CHANGELOG.md` with the release date.
    * Creates a new **Unreleased** section at the top of `CHANGELOG.md`.
 * Creates a PullRequest for the release branch:
    * Creates a new branch with a name like `release-0.5.x`.
    * This branch should be the same as `origin/master`.
    * Creates a pull request on the `release` branch.

### Acceptance

Now you should test this release. You can update to it by running `curl -L -s https://github.com/hortonworks/cloudbreak-deployer/archive/release-x.y.z.tar.gz | tar -xz -C $(dirname $(which cbd))`. Comment with LGTM (Looking Good To Me).

Once the PR is merged, CircleCI will:

* Create a new release on [GitHub releases tab](https://github.com/hortonworks/cloudbreak-deployer/releases), with the
 help of [gh-release](https://github.com/progrium/gh-release).
* Create the git tag with `v` prefix like: `v0.0.3`.
