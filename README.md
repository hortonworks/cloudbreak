Cloudbreak Deployer helps to deploy a cloudbreak environment into docker containers.

## Installation


## Core Containers

- **uaa**: OAuth Identity Server
- cloudbreak
- persicope
- uluwatu
- sultans

## System Level Containers

- consul: Service Registry
- registrator: automatically registers/deregisters containers into consul

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
git fetch && git checkout -b release-0.0.3
echo '0.0.3' > VERSION

# edit CHANGELOG.md

git commit -m "release 0.0.3" VERSION CHANGELOG.md
git push origin release-0.0.3
hub pull-request -b release -m "release 0.0.3"
```

## Credits

This tool, and the PR driven release, is very much inspired by [glidergun](https://github.com/gliderlabs/glidergun). Actually it
could be a fork of it. The reason it’s not a fork, because we wanted to have our own binary with all modules
built in, so only a single binary is needed.
