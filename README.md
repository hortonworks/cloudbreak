Cloudbreak Deployer helps to deploy a cloudbreak environment into docker containers.

## Requirements

Right now **Linux** and **OSX** 64 bit binaries are released from Cloudbreak Deployer. For anything else we will create a special docker container.
The deployment itself needs only **Dcoker 1.5.0** or later.

## Installation

To install Cloudbreak Deployer, you just have to unzip the platform specific
single binary to your PATH. The one-liner way is:

```
curl https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/master/install | bash
```

## Configuration

Configuration is based on environment variables. Cloudbreak Deployer always forks a new
bash subprocess **without inheriting env vars**. The only way to set env vars relevant to 
Cloudbreak Deployer is to set them in a file called `Profile`.

Actually the foolowing env vars _are_ inherited: 
- `HOME`
- `DEBUG`
- `TRACE`
- `CBD_DEFAULT_PROFILE`


### Env specific Profile

`Profile` is always sourced. For example If you have env specific configurations: prod you need
2 steps:

- create a file called `Profile.prod`
- set the `CBD_DEFAULT_PROFILE` env variable.

To use a specific profile once:
```
CBD_DEFAULT_PROFILE=prod cbd some_commands
```

For permanent setting you can `export CBD_DEFAULT_PROFILE=prod` in your `.bash_profile`.

## Debug

If you want to have more detailed output set the `DEBUG` env variable to non-zero:
```
DEBUG=1 cbd some_command
```

You can also use the `doctor` command to diagnose your environment:
```
cbd doctor
```

## Update

The tool is capable of upgrade itself:
```
cbd update
```

## Core Containers

- **uaa**: OAuth Identity Server
- cloudbreak
- persicope
- uluwatu
- sultans

## System Level Containers

- consul: Service Registry
- registrator: automatically registers/deregisters containers into consul

## Contribution

Development process should happen on separate branches. Then a pull-request should be opened as usual.
To validate the PR the binari `cbd` tool will be tested. Its built by CircleCI for each branch.
If you want to test the binary CircleCI built from your branch named `fix-something`, 

```
cbdl update-snap fix-something
```

## Testing

Shell scripts shouldn’t be exceptions when it comes to unit testing. [basht](https://github.com/progrium/basht)
is used for testing. See the reasoning about: [why not bats or shunit2](https://github.com/progrium/basht#why-not-bats-or-shunit2)

Please cover your bahs functions with unit tests.

## Release Process of Clodbreak Deployer tool

the master branch is always built on [CircleCI](https://circleci.com/gh/sequenceiq/cloudbreak-deployer).
When you wan’t a new release, all you have to do:

- create a PullRequest for the release branch:
  - make sure you change the `VERSION` file
  - update `CHANGELOG.md` with the release date
  - create a new **Unreleased** section in top of `CHANGELOG.md`

Once the PR is merged, CircleCI will:
- create a new release on [github releases tab](https://github.com/sequenceiq/cloudbreak-deployer/releases), with the help of the [gh-release](https://github.com/progrium/gh-release).
- it will create the git tag with `v` prefix like: `v0.0.3`

Sample command when version 0.0.3 was released:

```
export VER="${OLD_VER%.*}.$((${OLD_VER##*.}+1))"
export REL_DATE="[v${VER}] - $(date +%Y-%m-%d)"
git fetch && git checkout -b release-${VER}
echo $VER > VERSION

# edit CHANGELOG.md
sed -i "s/## Unreleased/## $REL_DATE/" CHANGELOG.md
echo -e '## Unreleased\n\n### Fixed\n\n### Added\n\n### Removed\n\n### Changed\n'| cat - CHANGELOG.md | tee CHANGELOG.md

git commit -m "release $VER" VERSION CHANGELOG.md
git push origin release-$VER
hub pull-request -b release -m "release $VER"
```

## Credits

This tool, and the PR driven release, is very much inspired by [glidergun](https://github.com/gliderlabs/glidergun). Actually it
could be a fork of it. The reason it’s not a fork, because we wanted to have our own binary with all modules
built in, so only a single binary is needed.
