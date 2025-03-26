# Dev Environment Setup
This document briefly provides the instructions to setup local development environment. is based upon the primary [README.md](https://github.infra.cloudera.com/cloudbreak/cloudbreak/blob/master/README.md) and is more practical set of instructions that will complete the setup. 
## Install CBD
Follow these steps to install first time
`git pull` or `git clone` the latest cloudbreak-deployer from [github](https://github.infra.cloudera.com/cloudbreak/cloudbreak-deployer.git)

Do the following to install the CBD on local

```
mkdir cbd-local
cd cbd-local
curl -s https://github.infra.cloudera.com/raw/cloudbreak/cloudbreak-deployer/master/install-dev | sh && cbd --version
```

Then create a file Profile and copy the following content to it:

```
export CB_LOCAL_DEV_LIST=
export UAA_DEFAULT_SECRET= < default secret value>

export AWS_ACCESS_KEY_ID= <aws key id>
export AWS_SECRET_ACCESS_KEY= <aws secret access key>
export CB_AWS_ACCOUNT_ID= <aws account id>

export CLOUDBREAK_SRC= <Your path to CloudBreak source code e.g./Users/ujjwal/cloudera/src/cloudbreak>

export CB_SCHEMA_SCRIPTS_LOCATION=$CLOUDBREAK_SRC/core/src/main/resources/schema
export DATALAKE_SCHEMA_SCRIPTS_LOCATION=$CLOUDBREAK_SRC/datalake/src/main/resources/schema
export ENVIRONMENT_SCHEMA_SCRIPTS_LOCATION=$CLOUDBREAK_SRC/environment/src/main/resources/schema
export FREEIPA_SCHEMA_SCRIPTS_LOCATION=$CLOUDBREAK_SRC/freeipa/src/main/resources/schema
export PERISCOPE_SCHEMA_SCRIPTS_LOCATION=$CLOUDBREAK_SRC/autoscale/src/main/resources/schema
export REDBEAMS_SCHEMA_SCRIPTS_LOCATION=$CLOUDBREAK_SRC/redbeams/src/main/resources/schema
export EXTERNALIZEDCOMPUTE_SCHEMA_SCRIPTS_LOCATION=$CLOUDBREAK_SRC/externalized-compute/src/main/resources/schema
export REMOTE_ENVIRONMENT_SCHEMA_SCRIPTS_LOCATION=$CLOUDBREAK_SRC/environment-remote/src/main/resources/schema

export ULU_SUBSCRIBE_TO_NOTIFICATIONS=true
export CB_INSTANCE_UUID=$(uuidgen | tr '[:upper:]' '[:lower:]')
export CB_INSTANCE_NODE_ID=5743e6ed-3409-420b-b08b-f688f2fc5db1
export PUBLIC_IP=localhost
export VAULT_AUTO_UNSEAL=true
export DPS_VERSION=2.0.0.0-142
```

Execute on command prompt: `cbd restart`

This will do the following:

Install the CBD using installer. Containers for Cloudbreak environment. Install the vault and configure the vault password. It will also install certs in the CBD-LOCAL folder

Create a file cbd-local/etc/license.txt  and copy the content from [here](https://github.infra.cloudera.com/raw/Starship/cmf/cdpd-master/web/dev_configs/test-license.txt.asc "https://github.infra.cloudera.com/raw/Starship/cmf/cdpd-master/web/dev_configs/test-license.txt.asc") in the file.

## Update Latest CBD
In most cases `cbd update-to-dev` command should get the latest published CBD binary from build repository.
However another way to install from Source may be required.
Use these steps to build CBD from the source.
Change directory using `cd cloudbreak-deployer` and execute the release-docker target using make. 
This is the easiest way to build CBD as it will use the build container with all necessary build dependencies. 
```
make release-docker
which cbd
cp build/Darwin/cbd /usr/local/bin/
```
And then start the environment in local docker container as per the Profile configurations.
```
cbd restart
```
Once all the containers have come up you can test.
Test the environment by opening the control panel console app from [Locally Cloudera Management Console http://localhost/](http://localhost)

## Configure IntelliJ

Create a file idea.env in cbd-local with following values:

```
AWS_ACCESS_KEY_ID= <aws key id>
AWS_SECRET_ACCESS_KEY=<aws secret access key>
CB_AWS_ACCOUNT_ID= <aws account id>
AWS_STS_REGIONAL_ENDPOINTS=regional
VAULT_ROOT_TOKEN=
AWS_USE_FIPS_ENDPOINT=false
CM_PRIVATE_REPO_USER=cloudbreak-deployer
CM_PRIVATE_REPO_PASSWORD=<repo password>
CLUSTERPROXY_ENABLED=false
ALTUS_UMS_HOST=localhost
GATEWAY_CERT_GENERATION_ENABLED=false
CB_DB_PORT_5432_TCP_ADDR=localhost
CB_DB_PORT_5432_TCP_PORT=5432
DATALAKE_DB_PORT_5432_TCP_ADDR=localhost
DATALAKE_DB_PORT_5432_TCP_PORT=5432
REDBEAMS_PORT_5432_TCP_ADDR=localhost
REDBEAMS_DB_PORT_5432_TCP_PORT=5432
CB_CERT_DIR=/Users/ujjwal/cloudera/cbd-local/certs
AUTH_CONFIG_DIR=/Users/ujjwal/cloudera/cbd-local
CB_IDENTITY_SERVER_URL=http://localhost:8089
CB_ENABLEDPLATFORMS=AWS,AZURE,GCP,MOCK,YARN


```

Create another file datalake.env with following value

```
SERVER_PORT=8086
```

Configure intelliJ Plugin = envfile plugin to IntelliJ open *settings>plugin* select *marketplace* and search for `EnvFile` plugin

Select Services from the side bar. For each servince in CloudBreak, DatalakeApplication, EnvironmentApplication, FreeIPAApplication, MockInfrastructureApplication, MockThunderheadApplication.

And select each service's *edit configuration* select Enable EnvFile and add [+] .env file `cbd-local/idea.env`

DatalakeApplication also needs the env files `cbd-local/datalake.env`

## Setup Local Profile

### configure CDP config & cred

The mock access key for a local CB can be generated by visiting this in the browser:

https://localhost/thunderhead/auth/mockkey/TENANT/EMAIL

[https://localhost/thunderhead/auth/mockkey/cloudera/upriyadarshan@cloudera.com](https://localhost/thunderhead/auth/mockkey/cloudera/upriyadarshan@cloudera.com)

Whatever values you previously picked when you logged into the UI (https://localhost/) the first time, the same ones shall be also included as TENANT and EMAIL in the above URL. The response will contain the access key for CDP CLI when targeting local CB.

Technically, TENANT and EMAIL are used to construct a mock user CRN:

crn:cdp:iam:us-west-1:TENANT:user:EMAIL

the user crn is encoded with base64, and that becomes the apikeyid (MOCK ACCESS KEY). The privatekey returned in the above mock endpoint is a fixed string "nHk...d2g=" that is otherwise completely ignored in CB for authentication.

Introduce a new Profile section named local in ~/.cdp/config:

```
[profile local]
form_factor = public
endpoint_url = http://localhost
cdp_endpoint_url = http://localhost
```

and provide credentials for this profile in ~/.cdp/credentials:

```
[local]
cdp_access_key_id = YOUR_MOCK_ACCESS_KEY
cdp_private_key = YOUR_MOCK_PRIVATE_KEY
```

where YOUR_MOCK_ACCESS_KEY is the value generated with the above mock endpoint URL.

Once configured, `local` can be used as profile for all CDP commands and the command will target the Configured Endpoint (on localhost) along with credentials.

### Create AWS Access Keys

To create an AWS access key and secret, follow these steps:

1. Sign in to the AWS Management Console : Use your credentials to log in.
2. Navigate to the IAM Console : Go to the IAM Management Console and select "Users" from the sidebar.
3. Select a User : Click on the user for whom you want to create the access key.
4. Create Access Key : Under the "Security credentials" tab, click "Create access key."

* Follow the prompts to generate the key.

1. Save the Keys : Once created, you'll see the access key ID and secret access key. Save these securely, as the secret key will not be shown again.

### Configure CBD Profile

Update `cbd-local\Profile` file with following environment variables with the AWS Access key and secret obtained from AWS:

```
export CB_AWS_ACCOUNT_ID="YOUR_AWS_ACCOUNT_ID"
export AWS_ACCESS_KEY_ID="YOUR_ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="YOUR_SECRET_KEY"
```

### Create External ID credentials in AWS

1. Create a policy suitable for cross account access: Since cloudera management control panel uses Customer's cloud providers account it is important to establish this cross account access between and this policy is pre-requisite for things to work.
   The policy is minimal, it can be found in the source cloudbreak/cloud-aws-common/src/main/resources/definitions/

   [aws-environment-minimal-policy.json](https://github.infra.cloudera.com/cloudbreak/cloudbreak/blob/master/cloud-aws-common/src/main/resources/definitions/aws-environment-minimal-policy.json) <-- for AWS commerial Cloud (Preferred for Dev)

   [aws-gov-environment-minimal-policy.json](https://github.infra.cloudera.com/cloudbreak/cloudbreak/blob/master/cloud-aws-common/src/main/resources/definitions/aws-gov-environment-minimal-policy.json) <-- for AWS GOV Cloud

2. Create an AWS IAM External Access Role And Credential in Cloudera Manageement Console:

   Open Cloudera managment console on http://localhost : select Shared Resources -> Credentials.
   Provide the credential name and copy the External ID

   Open AWS console open IAM -> roles: create new Role
   Choose AWS Account ...
   in Options: Require External ID copy the External ID copied from Cloudera Credentials
   Add the following policies to this role

   ```
   Cross Account Access Policy (defined in the previous step)
   ```

Copy the ARN of this new Role and copy it in the Cloudera Console's Credentials and save it.

NOTE: this credential's name is used as `--credential-name` in CDP commands.


# Test the environment terminal

```
cdp environments list-environments --profile local
```

# Reference

[https://cloudera.atlassian.net/wiki/spaces/ENG/pages/10177871890/SDX-Infra+Developer+Onboarding#Setup-Cloudbreak-deployer](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/10177871890/SDX-Infra+Developer+Onboarding#Setup-Cloudbreak-deployer)

[https://github.infra.cloudera.com/cloudbreak/cloudbreak#cloudbreak-deployer](https://github.infra.cloudera.com/cloudbreak/cloudbreak#cloudbreak-deployer)

[https://github.infra.cloudera.com/cloudbreak/cloudbreak/blob/master/README.md](https://github.infra.cloudera.com/cloudbreak/cloudbreak/blob/master/README.md)
