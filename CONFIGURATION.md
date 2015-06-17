# Configuration

Configuration is based on environment variables. Cloudbreak Deployer always forks a new bash subprocess **without
inheriting environment variables**. The only way to set ENV vars relevant for Cloudbreak Deployer is to set them
in a file called `Profile`.

To see all available config variables with their default value:

```
cbd env show
```

The `Profile` will be simple **sourced** in bash terms, so you can use the usual syntaxes to set config values:

```
export MY_VAR=some_value
export OTHER_VAR=dunno
```

# Env specific Profile

Let’s say you want to use a different version of Cloudbreak for **prod** and **qa** profile.
You can specify the Docker image tag via: `DOCKER_TAG_CLOUDBREAK`.
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

# Available Configurations

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
- **CB_TEMPLATE_DEFAULTS** : tbd 
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

## Caveats

The **Cloudbreak Deployer** tool opens a clean bash subshell, without inheriting environment vars.
Actually the foolowing environment vars _are_ inherited:

- `HOME`
- `DEBUG`
- `TRACE`
- `CBD_DEFAULT_PROFILE`
- all `DOCKER_XXX`
