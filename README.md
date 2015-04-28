Cloudbreak Deployer helps to deploy a Cloudbreak environment into Docker containers.
For recent changes please check the [CHANGELOG.md](https://github.com/sequenceiq/cloudbreak-deployer/blob/master/CHANGELOG.md).

## Requirements

Currently only **Linux** and **OSX** 64 bit binaries are released for Cloudbreak Deployer. For anything else we will create a special Docker container.
The deployment itself needs only **Docker 1.5.0** or later.

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

All Cloudbreak components and the backend database is running inside containers.
To download all the needed Docker images, with the correct tag, run the command:

```
cbd pull
```

It will take some time, depending on your network connection, so you can grab a cup of coffee.

### Migrate the databases
Create the database schema or migrate it to the latest version:

```
cbd startdb
cbd migrate cbdb up
```

Verify that all scripts have been applied:
```
cbd migrate cbdb status
```


```
cbd generate
cbd migrate cbdb up
```

### Deploy Cloudbreak

To start all the containers run:

```
cbd start
```
If one of the container is already running, it won’t be started again.

### Watch the logs

```
cbd logs
```
## Configuration

Configuration is based on environment variables. Cloudbreak Deployer always forks a new bash subprocess **without inheriting environment variables**. The only way to set ENV vars relevant for Cloudbreak Deployer is to set them in a file called `Profile`.

To see all available config variables with their default value:

```
cbd env show
```

The `Profile` will be simple **sourced** in bash terms, so you can use the usual syntaxes to set config values:

```
export MY_VAR=some_value
export OTHER_VAR=dunno
```

## Default Credentials

If you check the output of `cbd env` you can see the default principal/credential combination:
- user: **admin@example.com**
- user: **cloudbreak**

These values are generated in the `uaa.yml` end section. To change these values, add 2 lines into your Profile:

```
export UAA_DEFAULT_USER_EMAIL=myself@example.com
export UAA_DEFAULT_USER_PW=demo123
```
and than you need to recreate configs:
```
rm *.yml
cbd generate
```

### Env specific Profile

Let’s say you want to use a different version of Cloudbreak `DOCKER_TAG_CLOUDBREAK` for **prod** and **qa** profile.
`Profile` is always sourced, so you will have two env specific configurations:
- `Profile.dev`
- `Profile.qa`

For prod you need:

- create a file called `Profile.prod`
- write the env specific `export DOCKER_TAG_CLOUDBREAK=0.3.99` into `Profile.prod`
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

The tool is capable to upgrade itself to a newer version.

```
cbd update
```

## Core Cloudbreak Containers

- **uaa**: OAuth Identity Server
- cloudbreak - the Cloudbreak app
- persicope - the Periscope app
- uluwatu - Cloudbreak UI
- sultans - user management

## System Level Containers

- consul: Service Registry
- registrator: automatically registers/deregisters containers into Consul

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
cbdl update-snap fix-something
```

## Testing

Shell scripts shouldn’t raise exceptions when it comes to unit testing. [basht](https://github.com/progrium/basht) is used for testing. See the reasoning: [why not bats or shunit2](https://github.com/progrium/basht#why-not-bats-or-shunit2)

Please cover your bash functions with unit tests and run test with:

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

## Release Process of the Clodbreak Deployer tool

The master branch is always built on [CircleCI](https://circleci.com/gh/sequenceiq/cloudbreak-deployer).
When you wan’t a new release, all you have to do:

- create a PullRequest for the release branch:
  - make sure you change the `VERSION` file
  - update `CHANGELOG.md` with the release date
  - create a new **Unreleased** section in top of `CHANGELOG.md`

Once the PR is merged, CircleCI will:
- create a new release on [GitHub releases tab](https://github.com/sequenceiq/cloudbreak-deployer/releases), with the help of [gh-release](https://github.com/progrium/gh-release).
- it will create the git tag with `v` prefix like: `v0.0.3`

Sample release command(s) of version 0.0.3 release:

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

## Caveats

The **Cloudbreak Deployer** tool opens a clean bash subshell, without inheriting ENV vars.
Actually the foolowing ENV vars _are_ inherited: 

- `HOME`
- `DEBUG`
- `TRACE`
- `CBD_DEFAULT_PROFILE`
- all `DOCKER_XXX`

## Credits

This tool, and the PR driven release, is inspired from [glidergun](https://github.com/gliderlabs/glidergun). Actually it
could be a fork of it. The reason it’s not a fork, because we wanted to have our own binary with all modules
built in, so only a single binary is needed.
