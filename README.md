Cloudbreak Deployer helps to deploy a cloudbreak environment into docker containers.
For recent changes please check [CHANGELOG.md](https://github.com/sequenceiq/cloudbreak-deployer/blob/master/CHANGELOG.md).

## Requirements

Right now **Linux** and **OSX** 64 bit binaries are released from Cloudbreak Deployer. For anything else we will create a special docker container.
The deployment itself needs only **Dcoker 1.5.0** or later.

## Installation

To install Cloudbreak Deployer, you just have to unzip the platform specific
single binary to your PATH. The one-liner way is:

```
curl https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/master/install | sh && cbd --version
```

## Usage

**cbd** will generate some config files, and will download supporting binaries. It is
advised that you create a dedicated directory for it:

```
mkdir cloudbreak-deployment
cd cloudbreak-deployment
```

### Initialize
First lets initialize your directory by creating a `Profile`
```
cbd init
```

### Pull Docker images

All cloudbreak componont, and its supporting db is running in a container.
To download all needed Docker image, with correct tag, run the command:
```
cbd pull
```

It will take some minutes, depending on your network conditions, so you
can grab a cup of coffee.

### Deploy cloudbreak

To start all the containers run:
```
cbd start
```
If one of the containers are already running, it won’t be started again.

### Whatch the logs

```
cbd logs
```
## Configuration

Configuration is based on environment variables. Cloudbreak Deployer always forks a new
bash subprocess **without inheriting env vars**. The only way to set env vars relevant to 
Cloudbreak Deployer is to set them in a file called `Profile`.

The `Profile` will be simple **sourced** by bash terms, so you can use the usual syntax
to set config values:

```
export MY_VAR=some_value
export OTHER_VAR=dunno
```

### Env specific Profile

Let’s say you want to use a different `DOCKER_TAG_CLOUDBREAK` for **prod** and **qa** profile.
`Profile` is always sourced, so you will have two env specific configurations:
- `Profile.dev`
- `Profile.qa`

For prod you need:

- create a file called `Profile.prod`
- wirte the env specific `export DOCKER_TAG_CLOUDBREAK=0.3.99` into `Profile.prod`
- set the env variable: `CBD_DEFAULT_PROFILE=prod`

To use the `prod` specific profile once:
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

To build the project
```
# make deps needed only once
make deps

make instal
```

To run the unit tests:
```
make tests
```

If you want to test the binary CircleCI built from your branch named `fix-something`, 
To validate the PR the binary `cbd` tool will be tested. Its built by CircleCI for each branch.

```
cbdl update-snap fix-something
```

## Testing

Shell scripts shouldn’t be exceptions when it comes to unit testing. [basht](https://github.com/progrium/basht)
is used for testing. See the reasoning about: [why not bats or shunit2](https://github.com/progrium/basht#why-not-bats-or-shunit2)

Please cover your bahs functions with unit tests.

running test performed by:
```
make tests
```

# Configuration

## SMTP

Put these lines into your `Profile`
```
export CLOUDBREAK_SMTP_SENDER_USERNAME=
export CLOUDBREAK_SMTP_SENDER_PASSWORD=
export CLOUDBREAK_SMTP_SENDER_HOST=
export CLOUDBREAK_SMTP_SENDER_PORT=
export CLOUDBREAK_SMTP_SENDER_FROM=
```

### List of configurations

- **CB_AWS_AMI_MAP** : tbd 
- **CB_AZURE_IMAGE_URI** : tbd 
- **CB_BLUEPRINT_DEFAULTS** : tbd 
- **CB_DB_ENV_DB** : tbd 
- **CB_DB_ENV_PASS** : tbd 
- **CB_DB_ENV_USER** : tbd 
- **CB_GCP_SOURCE_IMAGE_PATH** : tbd 
- **CB_HBM2DDL_STRATEGY** : tbd 
- **CB_OPENSTACK_IMAGE** : tbd 
- **DOCKER_TAG_ALPINE** : tbd 
- **DOCKER_TAG_CBSHELL** : tbd 
- **DOCKER_TAG_CLOUDBREAK** : tbd 
- **DOCKER_TAG_CONSUL** : tbd 
- **DOCKER_TAG_PERISCOPE** : tbd 
- **DOCKER_TAG_POSTGRES** : tbd 
- **DOCKER_TAG_REGISTRATOR** : tbd 
- **DOCKER_TAG_SULTANS** : tbd 
- **DOCKER_TAG_UAA** : tbd 
- **DOCKER_TAG_ULUWATU** : tbd 
- **PERISCOPE_DB_HBM2DDL_STRATEGY** : tbd 


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
export OLD_VER=$(cat VERSION)
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

## Ceveats

The **Cloudbreak Deployer** tool opens a clean bash subshell, without inheriting env vars.
Actually the foolowing env vars _are_ inherited: 

- `HOME`
- `DEBUG`
- `TRACE`
- `CBD_DEFAULT_PROFILE`
- all `DOCKER_XXX`

## Credits

This tool, and the PR driven release, is very much inspired by [glidergun](https://github.com/gliderlabs/glidergun). Actually it
could be a fork of it. The reason it’s not a fork, because we wanted to have our own binary with all modules
built in, so only a single binary is needed.
