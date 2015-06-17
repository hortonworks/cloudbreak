## Unreleased

### Fixed

### Added

- Baywatch default parameters added: `CB_BAYWATCH_ENABLED` and`CB_BAYWATCH_EXTERN_LOCATION`

### Removed

### Changed

## [v0.5.3] - 2015-06-03

### Fixed

- One-liner installer fixed, to work if previous cbd exists on path.
- `cbd update` upstream changes on go-bahser broke the selfupdate functionality
- In some environment cloudbreak starts really slow. See: [details](http://stackoverflow.com/questions/137212/how-to-solve-performance-problem-with-java-securerandom), see: [commit](https://github.com/sequenceiq/docker-cloudbreak/commit/e00581d04fb14f28f778cf71253f2c8fa0d704ae)

### Added

- New release proposal can be done by `make release-next-ver`

### Removed

### Changed

## [v0.5.2] - 2015-05-21

### Fixed

### Added

### Removed

### Changed

- Command `doctor` hints to run boot2docker shellinit if env is unset
- Command `init` in case of OSX, DOCKER_XXX envs are initialized in local profile (Profile)
- Default docker images are updated to:
    - sequenceiq/cloudbreak:0.5.93
    - sequenceiq/cbdb:0.5.92
    - sequenceiq/uluwatu:0.5.28
    - sequenceiq/sultans:0.5.3
    - sequenceiq/periscope:0.5.5


## [v0.5.1] - 2015-05-18

### Fixed

- Issue #55: Sed handles more robust the issue with: curl includes an extra CR char in header fields.

### Added

### Removed

- deployer doesn’t specify cloud specific image defaults. If left empty, they fall back
  to defaults specified in [java code](https://github.com/sequenceiq/cloudbreak/blob/master/core/src/main/java/com/sequenceiq/cloudbreak/EnvironmentVariableConfig.java#L46-L49)
    - CB_AZURE_IMAGE_URI
    - CB_AWS_AMI_MAP
    - CB_OPENSTACK_IMAGE
    - CB_GCP_SOURCE_IMAGE_PATH

### Changed

- Command `logs` got usage example for specifying servies as filter
- Default docker images are updated to:
    - sequenceiq/cloudbreak:0.5.49
    - sequenceiq/uluwatu:0.5.16
    - sequenceiq/sutans:0.5.2

## [v0.5.0] - 2015-05-08

### Fixed

- Command `pull` generates yaml files in case they are missing #31

### Added

- Command `login` Shows Uluwatu login url and credentials
- Command `regenerate` deletes and generates docker-compose.yml and uaa.yml
- Command `delete` added: deletes yamls and dbs
- Command `cloudbreak-shell` added, right now it internale use DEBUG=1 fn fn-call
- Command `version` does correct [Semantic Versioning](http://semver.org) check to advise an upgrade
- Command `generate` checks and shows if Profile change would result in yaml change.
- Command `start`: prints uluwatu url and credential hint
- Command `doctor`: fixes boot2docker date/time if not the same as on the host
- Internal command: `browse` added to be able to automatically open a browser to a specified url.
- Mini Getting Started guide added into README
- `make dev-debug` installs a special cbd on local OSX, which doesn’t includes *.bash scrips, only refers them
   by path. No need to `make dev` to test small changes in bash scripts.
- Load AWS key and AWS id from Profile
- Command `init` helps to guess the PUBLIC_IP in public clouds: google, amazon

### Removed

### Changed

- Command `cbd env export` adds export to the begining of each line
- cbd logs accepts optional [services] parameter
- docker-compose uses `cbreak_` prefix for container naming instead of the directory name
- Command `generate` prints out some more usefull info
- uaa.yml generation wont overwrite, just instruct to move existing file (like docker-compose.yml generation)
- Command `init` hint fixed on linux.
- Command `init` advise to run `generate` if it finds a Profile
- Command `init` set PRIVATE_IP the same as PUBLIC_IP for boot2docker
- Command `migrate` is introduced for db migration see `Migrate the databases` section of README
- Command `startdb` starts the cbdb and pcdb database containers only
- Databases are not deleted after boot2docker restart
- Import ULU_HOST_ADDRESS and ULU_SULTANS_ADDRESS from Profile

## [v0.1.0] - 2015-04-16

### Fixed

- Selfupdate updates the actual running binary intead of the fixed /us/local/bin/cbd
- SMTP default port is 25, to fix number conversion exception

### Added

- Command `init` creates Profile
- Install cbd to a directory which is available on $PATH
- Docker based test for the one-liner install from README.md: `make install-test`

### Removed

- `update-snap` command removed, replaced by parametrized `update`

### Changed

- Cloudbreak/Persicope/Uluwatu/Sultans Dcoker images upgraded to 0.4.x
- Use the built in 'checksum' function instead of the external 'shasum' to generate secrets
- Command `update` by default updates from latest Github release, parameter can point to branch on CircleCI
- DOCKER_XXX env varibles are inherited, so they not needed in Profile
- `generate` and compose specific commands are only available when `Profile` exists
- `generate` command genertes docker-compose.yml **and** uaa.yml
- `PRIVATE_IP` env var defaults to bridge IP (only PUBLC_IP is required in Profile)
- use **sulans-bin** docker image istead of sultans

## [v0.0.9] - 2015-04-14

### Fixed

- Bash 4.3 is included in the binary, extracted into .deps/bin upon start

### Added

### Removed

### Changed

## [v0.0.8] - 2015-04-13

### Fixed

- Fixing deps module, golang fn: checksum added
- CircleCI mdule defines required jq
- Fixing PATH issue for binary deps

### Added

- uaadb start added
- identity server start added
- `make dev` added to mac based development
- `pull` command added
- `logs` command added

### Removed

### Changed

- Docker containers are managed by **docker-compose**

## [v0.0.7] - 2015-03-26

### Fixed

### Added

- `make tests` runs unit tests
- docker unit tests are added
- start command added: WIP consul, registrator starts
- kill command addd: stops and removes cloudbreak specific containers
- SKIP_XXX skips the container start

### Removed

### Changed

- env command namespace is always exported, not only in DEBUG mode
- env export: machine friendly config list
- env show: human readable config list
- circle runs unit tests
- snapshot binaries include branch name in version string

## [v0.0.6] - 2015-03-25

### Fixed

- removed dos2unix dependency for the update command

### Added

- doctor command added
- docker-check-version command added
- cci-latest accepts branch as parameter, needed for PR testing
- export fn command in DEBUG mode
- export env command in DEBUG mode
- doctor: add instruction about setting DOCKER_XXX env vars in Profile
- info() function added to print green text to STDOUT

### Removed

### Changed

- HOME env var is also inherited (boot2docker version failed)
- release process fully automatized

## [v0.0.5] - 2015-03-23

- `update` command works without dos2unix

## [v0.0.4] - 2015-03-23

### Fixed

- debug function fixed
- DEBUG, TRACE and CBD_DEFAULT_PROFILE env vars are inherited

### Added

- Profile handling added with docs
- One-liner install added
- Docs: install and update process described
- Docs: release process described with sample git commands
- Print version number in debug mode
- `update-snap` downloads binary from latest os specific CircleCI binary artifact.

### Removed

### Changed

- Tool specific library renamed from cloudbreak.bash to deployer.bash

## [v0.0.6] - 2015-03-25

## [v0.0.3] - 2015-03-19

### Fixed

- `make release` creates binary with X.X.X version when on release branch otherwise X.X.X-gitrev

### Added

- Docs: release process described

### Removed

### Changed

## [v0.0.2] - 2015-03-19

### Added
Added
- selfupdate command
- gray debug to stderr

## [v0.0.1] - 2015-03-18

## Added

- help command added
- version command added
- Added --version
- CircleCI build
- Linux/Darwin binary releases on github
