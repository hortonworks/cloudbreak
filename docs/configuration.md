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

## Env specific Profile

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

## Available Configurations

### SMTP

If you want to change SMTP parameters, put the corresponding lines into your `Profile`. You can also see the default values of the parameters in the following box.
```
export CLOUDBREAK_SMTP_SENDER_USERNAME=
export CLOUDBREAK_SMTP_SENDER_PASSWORD=
export CLOUDBREAK_SMTP_SENDER_HOST=
export CLOUDBREAK_SMTP_SENDER_PORT=25
export CLOUDBREAK_SMTP_SENDER_FROM=
export CLOUDBREAK_SMTP_AUTH=true
export CLOUDBREAK_SMTP_STARTTLS_ENABLE=true
export CLOUDBREAK_SMTP_TYPE=smtp
```

###Access from custom domains

Cloudbreak deployer uses UAA as an identity provider and supports multi tenancy. In UAA terminology this is referred as identity zones. An identity zone is accessed through a unique subdomain. If the standard UAA responds to [https://uaa.10.244.0.34.xip.io](https://uaa.10.244.0.34.xip.io) a zone on this UAA would be accessed through [https://testzone1.uaa.10.244.0.34.xip.io](https://testzone1.uaa.10.244.0.34.xip.io).

If you want to use a custom domain for your identity or deployment, put the `UAA_ZONE_DOMAIN` line into your
`Profile`. You can see an example in the following box:
```
export UAA_ZONE_DOMAIN=my-subdomain.example.com
```

### Consul

[Consul](http://consul.io) is used for DNS resolution. All Cloudbreak related services are registered as
**someservice.service.consul**. Consul’s built in DNS server is able to “fall-back” on an other DNS server.
This option is called `-recursor`. Clodbreak Deployer first tries to discover the DNS settings of the host,
by looking for **nameserver** entry in `/etc/resolv.conf`. If it finds one consul will use it as a recursor,
otherwise **8.8.8.8** will be used.

For a full list of available consul config options, see the [docs](https://consul.io/docs/agent/options.html).

You can pass any additional consul configuration by defining a `DOCKER_CONSUL_OPTIONS` in `Profile`.

## Azure Resource manager command
- **cbd azure configure-arm**
- **cbd azure deploy-dash**
See the documentation [here](/azure_pre_prov/#azure-application-setup-with-cloudbreak-deployer).

## Caveats

The **Cloudbreak Deployer** tool opens a clean bash subshell, without inheriting environment variables.

Only the following environment variables _are_ inherited:

- `HOME`
- `DEBUG`
- `TRACE`
- `CBD_DEFAULT_PROFILE`
- all `DOCKER_XXX`
