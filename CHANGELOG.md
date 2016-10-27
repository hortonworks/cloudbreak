
# Change Log

The Change Log summarizes the changes in Cloudbreak.

## [v1.6.1] - 

### Fixed

- can send invite e-mails when authentication is not enabled

### Changed

- recipe create command in shell is unified with other commands

## [v1.6.0] - 2016-09-15

### Fixed

- region is not a required field when posting a new stack
- ability to create bigger than 999 GB disks for Azure
- proper handling of upstart config in Salt to avoid concurrency between sysv and upstart when restarting clusters
- hive-site xmls are properly generated on worker nodes with default blueprints

### Added

- Ambari and HDP versions are added to the /stack API call's response
- when deploying a new subnet in an existing VPC on AWS, the CIDR range is automatically calculated when not specified explicitly on the API
- ability to create clusters on GCP without public IP addresses and without firewall modifications
- added env variable to specify static shared volumes in Mesos clusters

### Changed

- updated Ambari version to 2.4.1.0
- salt password is generated per stack for salt user

## [v1.5.0] - 2016-08-31

### Fixed

- ClusterBootstrapperTest randomly fails
- slider does not work with ssl on python 2.7.9+
- fix Ambari address on UI for clusters that provisioned by an older version of Cloudbreak
- credential visibility on cluster details view
- "Keystone Version" dropdown layouts

### Added

- added HDP version field to RDS config
- ability to configure remote database for Ambari
- stacks/clusters are associated with Cloudbreak version
- new resource to configure LDAP service for Knox
- support of Google's spot instances
- support for c4 instance types in AWS
- enable auto recovery for HDP services on failure via blueprint
- ability to specify multiple subnets for the same VPC on AWS
- ability to configure default regions

### Changed

- reinstate recipes on Uluwatu UI
- increase Ambari password length and fix error message
- automatic blueprint update during startup
- on OpenStack stack name contains stack ID
- disable upgrade of saltstack
- remove unecessary `require` flags from API documentation
- improve security of salt communication

## [v1.4.0] - 2016-08-12

### Fixed

- remove invalid auto scaling group history check
- fix sync to avoid node count corruption of instance groups
- HDP on AWS can return 502 Bad Gateway while accessing Ambari
- fix Ambari server ip selection on AWS
- fix enable auto scaling with different user

### Added

- new RDS config resource
- shell commands for RDS config operations
- configurable image catalog
- allow dynamically change Ambari version
- added `canada` region to Azure
- add basic auth to Zeppelin, Zeppelin is secured with the same password like Ambari
- ability to configure a custom domain for the clusters with the CB_HOST_DISCOVERY_CUSTOM_DOMAIN env variable

### Changed

- update images for on-the-fly ambari update
- increase poll timeout for ARM
- disable automated map/reduce smoke test
- parallel command execution in Ambari
- update to Smartsense 1.3.0
- add admin user to `hadoop` and `hdfs` groups after Ambari install
- use only Cloudbreak address to connect Cloudbreak

## [v1.3.2]

### Fixed

- Ambari restart
- avoid duplicated alarmas
- before upscale the sync won't change the stack state
- upscale / downscale fix with Ambari 2.4

### Added

- reverse proxy for UI components
- new supported volume type on aws: ST1
- Spark reverse proxy settings
- Spark jobhistory server reverse proxy settings
- create admin user for Ambari views
- added group name to AWS instance Name tags
- add etl-edw default blueprint
- configure Hive RDS through Cloudbreak
- configurable default SmartSense configuration and installation
- add SmartSense server to a single noded hostgroup if possible
- generate SmartSense id as credential attribute
- set SmartSense capture schedule on hosts by recipe

### Changed

- update to Ambari 2.4.0.0
- use OpenJDK instead of Oracle JDK
- use Ambari password as default service password
- make Sultans base path configurable with '/sl' as default value
- create individual root path for Cloudbreak and Periscope
- set DNS TTL for AWS clients to <= 60s
- rewrite url if ambari sessionid appears in cookies
- remove MYSQL_SERVER component from blueprint if there is rds config present
- handle AWS account id as String during SmartSense id generation
- update proxy users hosts to avoid unauthorized connection for super-user in Oozie
- set ONLY_STACK_DEFAULTS_APPLY as default since the new recommendation strategy does not work for Falcon and Ozzie
- show blueprint name on review panel and hide hostroup panel by default
- show only the previously selected network on the review panel

### Removed

- Spark and Zeppelin nginx proxy settings

## [v1.3.0] - 2016-06-06

### Fixed

- delete cluster containers only if orchestrator type is container
- show reason of failed commands when using the shell
- login when password contains special characters
- textarea placeholder on Internet Explorer 11
- HDP repo verification on Uluwatu
- wrong instance metadata status during cluster sync
- manage platforms close button on UI
- null pointer during router creation on non existing Openstack network

### Added

- use salt bootstrap instead of cloudbreak bootstrap
- missing gateway port to stack response
- enable spot price instances
- AWS cluster creation with existing key pair
- AWS existing SSH key pair could be configured by credential command
- new description and output for CFN stack
- add ssh port into instance metadata
- seamless s3 connection
- custom CIDR validator for subnet
- custom tag to CloudFormation stack created by Cloudbreak
- start termination flow stops other running flows on the same stack
- Openstack API facing option

### Changed

- openstack network shell command improvement for the new network types
- ability to select where to put ambari server
- reorganize kerberos setup in Salt
- using private address if only private address is available
- always use ports on Openstack even there is no floating ip assigned
- floating IP shall not be mandatory, and do not create separate ports
- follow HBase port changes
- move variant from advanced option to the basic options page
- Spring update
- elastic ips are managed by CloudFormation
- sync starts automatically at startup
- read nginx SSL port from parameter instead of static 443
- sync to handle instances that are stopped on the provider side
- Openstack metadata collection handles manually terminated instances
- cleaned up Openstack resources in case of existing subnet

### Removed

- recipe and ssd config were removed
- docker properties removed from UI

## [v1.2.6] - 2016-05-19

### Changed

- use lates cloud images with ambari-agent:2.2.1-v20
- map public ips to vms in case of existing vpc

## [v1.2.5] - 2016-04-07

### Fixed

- slow lazy format on Azure
- mounted disks are not visible in containers

### Added

- assume role without adding the keys into cbd Profile
- ability to create cluster without public ip
- high availability blueprint validator
- support for older gcp projectids

## [v1.2.4] - 2016-03-22

### Fixed

- azure storage location
- openstack create network form
- azure create network form
- gcp disk type

### Added

- GCP subnetsupport  to shell

### Changed

- use the provided public network id for allocation floating ips
- allow 24 attached volumes for aws
- cleaned up openstack resources in case of existing subnet

## [v1.2.3] - 2016-03-22

### Fixed

- sudo right of CB user

### Added

- Azure default network

## [v1.2.2] - 2016-03-21

### Fixed

- credential validation in case of Mesos/Marathon

## [v1.2.1] - 2016-03-21

### Fixed

- live migration operations

## [v1.2.0] - 2016-03-18

### Fixed

- consul recursor now exculdes both docker ip and bridge ip to avoid recursive dns recursor chain
- docs fixed about getting default credentials (cbd login)
- updates cb-shell to 0.5.37 to fix ssl issues

### Added

- Command `cbd azure configure-arm` will create your arm application which can used by cloudbreak
- Command `cbd azure deploy-dash` will deploy a dash application in your Azure account
- Command `cbd start` will execute the migration by default. If SKIP_DB_MIGRATION_ON_START envvar set to true in Profile, the migration will be skipped
- Using Dns SRV record in our services instead of ambassador
- Using docker linking system in third party services instead of ambassador
- Integration tests are added, where cbd binary is called, not only sourced functions
- Docker based CentOS integration test make target added
- Uaa db migration
- SMTP default parameters added: `CLOUDBREAK_SMTP_AUTH` and `CLOUDBREAK_SMTP_STARTTLS_ENABLE` and `CLOUDBREAK_SMTP_TYPE`
- Local development Uluwatu configuration by ULUWATU_VOLUME_HOST environment variable
- Local development Sultans configuration by SULTANS_VOLUME_HOST environment variable
- install script for fixed version and install-latest for latest release added
- Each snapshot artifact is uploaded as http://public-repo-1.hortonworks.com/HDP/cloudbreak/cbd-snapshot-$(uname).tgz
- Configuration ability to enable or disable ssh fingerprint verification of virtual machines on GCP and AWS

### Removed

- Full removal of ambassador

### Changed

- `cbd start` doesn’t start if compose yaml regeneration is needed
- `cbd generate` is less verbose, diff doesnt shown
- `cbd doctor` shows diff if generate would change
- `cbd regenerate` creates backup files if changes detected
- sequenceiq/uaadb:1.0.1 is used instead of postgres:9.4.1

## [v1.0.3] - 2015-09-03

### Fixed

- Authentication error with `cloudbreak-shell` and `cloudbreak-shell-quiet` is fixed
- Command `cbd update <branch>` checks for artifact

### Added

- binary version of gnu-sed 4.2.2 is now included, to solve lot of osx/busybox issues
- consul recursor test are added

### Changed

- sequenceiq/cloudbreak image updated to 1.0.3

- debug() function made multiline capable. Use \n in messages
- refactor bridge ip discovery to run helper docker command only once
- consul recursor handling refactored to be more robust

## [v1.0.2] - 2015-08-25

### Added

- `DOCKER_CONSUL_OPTIONS` config option to provide arbitrary consul option

### Changed

- Fixed docker version checker to be 1.8.1 compatible. (docker added --format option)
- sequenceiq/cloudbreak image updated to 1.0.2
- consul image changed from sequenceiq/consul to gliderlabs/consul
- consul image updated to 0.5.2 (from 0.5.0)
- consul discovers host dns settings, and uses the configured nameserver as recursor

## [v1.0.0] - 2015-07-23

### Fixed

- GA Release

## [v0.5.8] - 2015-07-23

### Fixed

- Fix CircleCI release. CircleCI doesn’t allow --rm on docker run

## [v0.5.7] - 2015-07-23

### Fixed

- Fix make release dependency
- Fix CHANGELOG generation at `make release-next-ver` avoid inserting extra -e

## [v0.5.6] - 2015-07-23

### Added

- Release artifacts are published at public-repo-1.hortonworks.com/HDP/cloudbreak/

## [v0.5.5] - 2015-07-10

### Added

- Command `pull-parallel` added for quicker/simultaneous image pull
- Release process includes upload to public-repo s3 bucket

### Changed

- License changed from MIT to Apache v2
- release artifact includes additional files: license/readme/notes

## [v0.5.4] - 2015-07-03

### Added

- New `cbd-cleanup` command for removing old images or exited containers
- Baywatch default parameters added: `CB_BAYWATCH_ENABLED` and`CB_BAYWATCH_EXTERN_LOCATION`
- Logs are saved via lospout
- TLS client certificate needed by Cloudbreak is generated with `cbd generate`
- Command `aws delete-role ` added
- Command `aws generate-role` added
- Command `aws show-role` added
- Command `cloudbreak-shell` added
- Command `cloudbreak-shell-quiet` added
- Command `local-dev` added
- Command `token` added

### Changed

- AWS authentication env varibale is fixed to use the correct AWS_SECRET_ACCESS_KEY (instead the old AWS_SECRET_KEY)
- Using sequenceiq/ambassadord:0.5.0 docker image instead of progrium/ambassadord:latest

## [v0.5.3] - 2015-06-03

### Fixed

- One-liner installer fixed, to work if previous cbd exists on path.
- `cbd update` upstream changes on go-bahser broke the selfupdate functionality
- In some environment cloudbreak starts really slow. See: [details](http://stackoverflow.com/questions/137212/how-to-solve-performance-problem-with-java-securerandom), see: [commit](https://github.com/sequenceiq/docker-cloudbreak/commit/e00581d04fb14f28f778cf71253f2c8fa0d704ae)

### Added

- New release proposal can be done by `make release-next-ver`

## [v0.5.2] - 2015-05-21

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

### Removed

- deployer doesn’t specify cloud specific image defaults. If left empty, they fall back
  to defaults specified in [java code](https://github.com/sequenceiq/cloudbreak/blob/master/core/src/main/java/com/sequenceiq/cloudbreak/EnvironmentVariableConfig.java#L46-L49)
    - CB_AZURE_IMAGE_URI
    - CB_AWS_AMI_MAP
    - CB_OPENSTACK_IMAGE
    - CB_GCP_SOURCE_IMAGE_PATH

### Changed

- Command `logs` got usage example for specifying services as filter
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

### Changed

- Docker containers are managed by **docker-compose**

## [v0.0.7] - 2015-03-26

### Added

- `make tests` runs unit tests
- docker unit tests are added
- start command added: WIP consul, registrator starts
- kill command addd: stops and removes cloudbreak specific containers
- SKIP_XXX skips the container start

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

### Changed

- Tool specific library renamed from cloudbreak.bash to deployer.bash

## [v0.0.3] - 2015-03-19

### Fixed

- `make release` creates binary with X.X.X version when on release branch otherwise X.X.X-gitrev

### Added

- Docs: release process described

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
