## Unreleased

### Fixed

- Command `pull` generates yaml files in case they are missing #31

### Added

- Mini Getting Started guide added into README

### Removed

### Changed

- Command `generate` prints out some more usefull info
- uaa.yml generation wont overwrite, just instruct to move existing file (like docker-compose.yml generation)
- Command `init` hint fixed on linux.
- Command `init` advise to run `generate` if it finds a Profile

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
