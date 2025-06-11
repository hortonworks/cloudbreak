# Cloudbreak

- [Local Development Setup](#local-development-setup)
    * [Cloudbreak Deployer](#cloudbreak-deployer)
        + [Cloudbreak Service Ports](#cloudbreak-service-ports)
        + [Linux Difference](#linux-difference)
    * [IDEA](#idea)
        + [Check Out the Cloudbreak Repository](#check-out-the-cloudbreak-repository)
        + [Project Settings in IDEA](#project-settings-in-idea)
        + [Import Project](#import-project)
        + [Running Cloudbreak in IDEA](#running-cloudbreak-in-idea)
        + [Configure Before launch task](#configure-before-launch-task)
        + [Running Periscope in IDEA](#running-periscope-in-idea)
        + [Running Datalake in IDEA](#running-datalake-in-idea)
        + [Running FreeIPA in IDEA](#running-freeipa-in-idea)
        + [Running Redbeams in IDEA](#running-redbeams-in-idea)
        + [Running the Environment Service in IDEA](#running-the-environment-service-in-idea)
        + [Running the Remote Environment Service in IDEA](#running-the-remote-environment-service-in-idea)
        + [Running the Externalized Compute Service in IDEA](#running-the-externalized-compute-service-in-idea)
        + [Running Thunderhead Mock in IDEA](#running-thunderhead-mock-in-idea)
        + [Running Mock-Infrastructure in IDEA](#running-mock-infrastructure-in-idea)
    * [Command Line](#command-line)
        + [Running Cloudbreak from the Command Line](#running-cloudbreak-from-the-command-line)
        + [Running Periscope from the Command Line](#running-periscope-from-the-command-line)
        + [Running Datalake from the Command Line](#running-datalake-from-the-command-line)
        + [Running FreeIPA from the Command Line](#running-freeipa-from-the-command-line)
        + [Running Redbeams from the Command Line](#running-redbeams-from-the-command-line)
        + [Running the Environment Service from the Command Line](#running-the-environment-service-from-the-command-line)
        + [Running the Remote Environment Service from the Command Line](#running-the-remote-environment-service-from-the-command-line)
        + [Running the Externalized Compute Service from the Command Line](#running-the-externalized-compute-service-from-the-command-line)
        + [Running Thunderhead Mock from the Command Line](#running-thunderhead-mock-from-the-command-line)
        + [Running Mock-Infrastructure from the Command Line](#running-mock-infrastructure-from-the-command-line)
    * [Database Development](#database-development)
    * [Building](#building)
    * [How to Reach CM UI Directly (Not Through Knox)](#how-to-reach-cm-ui-directly-not-through-knox)
- [How to Contribute](#how-to-contribute)
    * [Appearance](#appearance)
    * [Coding Guidelines](#coding-guidelines)
    * [Catching Up](#catching-up)
    * [Additional Info](#additional-info)
- [Api definitions](#api-definitions)
- Documentation: https://docs.cloudera.com/management-console/cloud/index.html

### Sonar
[SonarLink](https://sonarqube.infra.cloudera.com/projects/favorite?search=UT_MASTER)

| **Service Name**       | Maintainability                                                                                                                                                                                                                                                                                               | Reliability                                                                                                                                                                                                                                                                                                    | Security                                                                                                                                                                                                                                                                                                 | Covereage                                                                                                                                                                                                                                                                                  |
|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Core                   | [![Maintainability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=CloudbreakCore_Service_UT_master&metric=sqale_rating&token=sqb_3f7eb39c8bddd392a8616bcde2ae4fa9dacfd695)](https://sonarqube.infra.cloudera.com/dashboard?id=CloudbreakCore_Service_UT_master)              | [![Reliability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=CloudbreakCore_Service_UT_master&metric=reliability_rating&token=sqb_3f7eb39c8bddd392a8616bcde2ae4fa9dacfd695)](https://sonarqube.infra.cloudera.com/dashboard?id=CloudbreakCore_Service_UT_master)             | [![Security Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=CloudbreakCore_Service_UT_master&metric=security_rating&token=sqb_3f7eb39c8bddd392a8616bcde2ae4fa9dacfd695)](https://sonarqube.infra.cloudera.com/dashboard?id=CloudbreakCore_Service_UT_master)             | [![Coverage](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=CloudbreakCore_Service_UT_master&metric=coverage&token=sqb_3f7eb39c8bddd392a8616bcde2ae4fa9dacfd695)](https://sonarqube.infra.cloudera.com/dashboard?id=CloudbreakCore_Service_UT_master)             |
| Datalake               | [![Maintainability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Datalake_Service_UT_master&metric=sqale_rating&token=sqb_e864369aa8075ab8e8cd484a990da528f8139abb)](https://sonarqube.infra.cloudera.com/dashboard?id=Datalake_Service_UT_master)                          | [![Reliability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Datalake_Service_UT_master&metric=reliability_rating&token=sqb_e864369aa8075ab8e8cd484a990da528f8139abb)](https://sonarqube.infra.cloudera.com/dashboard?id=Datalake_Service_UT_master)                         | [![Security Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Datalake_Service_UT_master&metric=security_rating&token=sqb_e864369aa8075ab8e8cd484a990da528f8139abb)](https://sonarqube.infra.cloudera.com/dashboard?id=Datalake_Service_UT_master)                         | [![Coverage](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Datalake_Service_UT_master&metric=coverage&token=sqb_e864369aa8075ab8e8cd484a990da528f8139abb)](https://sonarqube.infra.cloudera.com/dashboard?id=Datalake_Service_UT_master)                         |
| Environment            | [![Maintainability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Environment_Service_UT_master&metric=sqale_rating&token=sqb_4b0790ebecfee6e1ef099663fde2d08defba4df8)](https://sonarqube.infra.cloudera.com/dashboard?id=Environment_Service_UT_master)                    | [![Reliability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Environment_Service_UT_master&metric=reliability_rating&token=sqb_4b0790ebecfee6e1ef099663fde2d08defba4df8)](https://sonarqube.infra.cloudera.com/dashboard?id=Environment_Service_UT_master)                   | [![Security Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Environment_Service_UT_master&metric=security_rating&token=sqb_4b0790ebecfee6e1ef099663fde2d08defba4df8)](https://sonarqube.infra.cloudera.com/dashboard?id=Environment_Service_UT_master)                   | [![Coverage](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Environment_Service_UT_master&metric=coverage&token=sqb_4b0790ebecfee6e1ef099663fde2d08defba4df8)](https://sonarqube.infra.cloudera.com/dashboard?id=Environment_Service_UT_master)                   |
| Externalized Compute   | [![Maintainability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Externalized_Compute_Service_UT_master&metric=sqale_rating&token=sqb_e4aa7668e50d86741eb67997e03974045897f6a1)](https://sonarqube.infra.cloudera.com/dashboard?id=Externalized_Compute_Service_UT_master)  | [![Reliability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Externalized_Compute_Service_UT_master&metric=reliability_rating&token=sqb_e4aa7668e50d86741eb67997e03974045897f6a1)](https://sonarqube.infra.cloudera.com/dashboard?id=Externalized_Compute_Service_UT_master) | [![Security Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Externalized_Compute_Service_UT_master&metric=security_rating&token=sqb_e4aa7668e50d86741eb67997e03974045897f6a1)](https://sonarqube.infra.cloudera.com/dashboard?id=Externalized_Compute_Service_UT_master) | [![Coverage](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Externalized_Compute_Service_UT_master&metric=coverage&token=sqb_e4aa7668e50d86741eb67997e03974045897f6a1)](https://sonarqube.infra.cloudera.com/dashboard?id=Externalized_Compute_Service_UT_master) |
| Freeipa                | [![Maintainability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Freeipa_Service_UT_master&metric=sqale_rating&token=sqb_b184a7d06549370a694698bf7a99de000dfb24c7)](https://sonarqube.infra.cloudera.com/dashboard?id=Freeipa_Service_UT_master)                            | [![Reliability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Freeipa_Service_UT_master&metric=reliability_rating&token=sqb_b184a7d06549370a694698bf7a99de000dfb24c7)](https://sonarqube.infra.cloudera.com/dashboard?id=Freeipa_Service_UT_master)                           | [![Security Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Freeipa_Service_UT_master&metric=security_rating&token=sqb_b184a7d06549370a694698bf7a99de000dfb24c7)](https://sonarqube.infra.cloudera.com/dashboard?id=Freeipa_Service_UT_master)                           | [![Coverage](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Freeipa_Service_UT_master&metric=coverage&token=sqb_b184a7d06549370a694698bf7a99de000dfb24c7)](https://sonarqube.infra.cloudera.com/dashboard?id=Freeipa_Service_UT_master)                           |
| Redbeams               | [![Maintainability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Redbeams_Service_UT_master&metric=sqale_rating&token=sqb_b2cfff9cc2b3ee8bd8ecb109319f8ed3d4662e94)](https://sonarqube.infra.cloudera.com/dashboard?id=Redbeams_Service_UT_master)                          | [![Reliability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Redbeams_Service_UT_master&metric=reliability_rating&token=sqb_b2cfff9cc2b3ee8bd8ecb109319f8ed3d4662e94)](https://sonarqube.infra.cloudera.com/dashboard?id=Redbeams_Service_UT_master)                         | [![Security Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Redbeams_Service_UT_master&metric=security_rating&token=sqb_b2cfff9cc2b3ee8bd8ecb109319f8ed3d4662e94)](https://sonarqube.infra.cloudera.com/dashboard?id=Redbeams_Service_UT_master)                         | [![Coverage](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Redbeams_Service_UT_master&metric=coverage&token=sqb_b2cfff9cc2b3ee8bd8ecb109319f8ed3d4662e94)](https://sonarqube.infra.cloudera.com/dashboard?id=Redbeams_Service_UT_master)                         |
| Remote Environment     | [![Maintainability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Remote_Environment_Service_UT_master&metric=sqale_rating&token=sqb_49c013ba7bea80cad94382aeb0623a6f029144cf)](https://sonarqube.infra.cloudera.com/dashboard?id=Remote_Environment_Service_UT_master)      | [![Reliability Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Remote_Environment_Service_UT_master&metric=reliability_rating&token=sqb_49c013ba7bea80cad94382aeb0623a6f029144cf)](https://sonarqube.infra.cloudera.com/dashboard?id=Remote_Environment_Service_UT_master)     | [![Security Rating](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Remote_Environment_Service_UT_master&metric=security_rating&token=sqb_49c013ba7bea80cad94382aeb0623a6f029144cf)](https://sonarqube.infra.cloudera.com/dashboard?id=Remote_Environment_Service_UT_master)     | [![Coverage](https://sonarqube.infra.cloudera.com/api/project_badges/measure?project=Remote_Environment_Service_UT_master&metric=coverage&token=sqb_49c013ba7bea80cad94382aeb0623a6f029144cf)](https://sonarqube.infra.cloudera.com/dashboard?id=Remote_Environment_Service_UT_master)     |


# Local Development Setup
As of now this document focuses on setting up your development environment on macOS. You'll need Homebrew to install certain components in case you don't have them already. To get Homebrew please follow the installation instructions on the Homebrew homepage: https://brew.sh

As a prerequisite, you need to have Java 21 installed. You can choose from many options, including the Oracle JDK, Oracle OpenJDK, or an OpenJDK from any of several providers. For help in choosing your JDK, consult [Java is Still Free](https://medium.com/@javachampions/java-is-still-free-2-0-0-6b9aa8d6d244).

You'll need Docker. For Mac, use [Docker Desktop for Mac](https://docs.docker.com/docker-for-mac/install/). Please allocate at least 6 CPU and 12 GB Memory to the process. (Depends on that how many service running in your IntelliJ and in Docker containers)

> INFO: For quick setup instruction refer to [README-devnotes.md](README-devnotes.md)

## Cloudbreak Deployer

The simplest way to set up the working environment to be able to start Cloudbreak on your local machine is to use the [Cloudbreak Deployer](https://github.infra.cloudera.com/cloudbreak/cloudbreak-deployer).

First you need to create a sandbox directory which will store the necessary configuration files and dependencies of [Cloudbreak Deployer](https://github.infra.cloudera.com/cloudbreak/cloudbreak-deployer). This directory must be created outside the cloned Cloudbreak git repository:
```
mkdir cbd-local
cd cbd-local
```

The next step is to download the latest cloudbreak-deployer onto your machine:
```
curl -s https://github.infra.cloudera.com/raw/cloudbreak/cloudbreak-deployer/master/install-dev | sh && cbd --version
```

Add the following to the file named `Profile` under the `cbd-local` directory you have just created. Please note, when a `cbd` command is executed you should go to the deployment's directory where your `Profile` file is found (`cbd-local` in our example). The `CB_SCHEMA_SCRIPTS_LOCATION` environment variable configures the location of SQL scripts that are in the `core/src/main/resources/schema` directory in the cloned Cloudbreak git repository.
In a similar fashion, the rest of the other `*_SCHEMA_SCRIPTS_LOCATION` environment variables configure the locations of SQL scripts that are associated with those respective services.

Please note that the full path needs to be configured and env variables like `$USER` cannot be used. You also have to set a password for your local Cloudbreak in `UAA_DEFAULT_USER_PW`:

```
export CB_LOCAL_DEV_LIST=
export UAA_DEFAULT_SECRET=cbsecret2015
export CB_SCHEMA_SCRIPTS_LOCATION=/Users/YOUR_USERNAME/YOUR_PROJECT_DIR/cloudbreak/core/src/main/resources/schema
export DATALAKE_SCHEMA_SCRIPTS_LOCATION=/Users/YOUR_USERNAME/YOUR_PROJECT_DIR/cloudbreak/datalake/src/main/resources/schema
export ENVIRONMENT_SCHEMA_SCRIPTS_LOCATION=/Users/YOUR_USERNAME/YOUR_PROJECT_DIR/cloudbreak/environment/src/main/resources/schema
export FREEIPA_SCHEMA_SCRIPTS_LOCATION=/Users/YOUR_USERNAME/YOUR_PROJECT_DIR/cloudbreak/freeipa/src/main/resources/schema
export PERISCOPE_SCHEMA_SCRIPTS_LOCATION=/Users/YOUR_USERNAME/YOUR_PROJECT_DIR/cloudbreak/autoscale/src/main/resources/schema
export REDBEAMS_SCHEMA_SCRIPTS_LOCATION=/Users/YOUR_USERNAME/YOUR_PROJECT_DIR/cloudbreak/redbeams/src/main/resources/schema
export EXTERNALIZEDCOMPUTE_SCHEMA_SCRIPTS_LOCATION=/Users/YOUR_USERNAME/YOUR_PROJECT_DIR/cloudbreak/externalized-compute/src/main/resources/schema
export REMOTE_ENVIRONMENT_SCHEMA_SCRIPTS_LOCATION=/Users/YOUR_USERNAME/YOUR_PROJECT_DIR/cloudbreak/environment-remote/src/main/resources/schema
export ULU_SUBSCRIBE_TO_NOTIFICATIONS=true
export CB_INSTANCE_UUID=$(uuidgen | tr '[:upper:]' '[:lower:]')
export CB_INSTANCE_NODE_ID=5743e6ed-3409-420b-b08b-f688f2fc5db1
export PUBLIC_IP=localhost
export VAULT_AUTO_UNSEAL=true
export DPS_VERSION=2.0.0.0-142
```

If you want to use the `mock-infrastructure`, you need to add to the `Profile` the following:
```
export MOCK_INFRASTRUCTURE_HOST=localhost
```

If you want to save some memory then one or more services can be skipped in local runs like (see complete list of supported service names below):
```
export CB_LOCAL_DEV_LIST=periscope,distrox-api,environments2-api,datalake-api
```

If you are using AWS (commercial regions), then also add the following lines, substituting your control plane AWS account ID, and the AWS credentials that you have created for the CB role.

```
export CB_AWS_ACCOUNT_ID="YOUR_AWS_ACCOUNT_ID"
export AWS_ACCESS_KEY_ID="YOUR_ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="YOUR_SECRET_KEY"
```

Furthermore, in order to use AWS GovCloud, also add the following lines, again substituting your control plane AWS GovCloud account ID, and the AWS GovCloud credentials that you have created for the CB role.

```
export CB_AWS_GOV_ACCOUNT_ID="YOUR_AWS_GOVCLOUD_ACCOUNT_ID"
export AWS_GOV_ACCESS_KEY_ID="YOUR_ACCESS_KEY"
export AWS_GOV_SECRET_ACCESS_KEY="YOUR_SECRET_KEY"
```

At first, you should start every service from cbd to check that your cloud environment is set up correctly.
```
export CB_LOCAL_DEV_LIST=
```

When this setup works, you can remove services from cbd, and run them locally. For example, in order to run Cloudbreak, Periscope, Datalake, FreeIPA, Redbeams, Environment, Thunderhead Mock (and Thunderhead API), IDBroker Mapping Management, and Environments2 API services locally (from IDEA or the command line), put this into your `Profile`:
```
export CB_LOCAL_DEV_LIST=cloudbreak,periscope,datalake,freeipa,redbeams,environment,remote-environment,thunderhead-mock,thunderhead-api,idbmms,environments2-api
```

Containers for these applications won't be started and Uluwatu (or the `cdp` & `dp` CLI tools) will connect to Java processes running on your host.
You don't have to put all the applications into local-dev mode; the value of the variable could be any combination. The following service names are supported in `CB_LOCAL_DEV_LIST`:

- `audit`
- `audit-api`
- `cadence`
- `cloudbreak`
- `cluster-proxy`
- `core-gateway`
- `datalake`
- `datalake-api`
- `datalake-dr`
- `distrox-api`
- `environment`
- `remote-environment`
- `environments2-api`
- `freeipa`
- `idbmms`
- `mock-infrastructure`
- `periscope`
- `redbeams`
- `thunderhead-api`
- `thunderhead-mock`
- `workloadiam`

You need to log in to DockerHub:
```
docker login
```
And then provide your username and password.

Then run these commands:
```
cbd start
cbd logs cloudbreak
```

In case you see `org.apache.ibatis.migration.MigrationException` at the end of the logs run these commands to fix the DB and the re-run the previous section (`cbd start` and `logs`):
```
cbd migrate cbdb up
cbd migrate cbdb pending
```

For some reason if you encounter a similar problem with Periscope, Datalake, FreeIPA, Redbeams, or Environment, then run the following commands, and you can restart the Cloudbreak Deployer:
```
cbd migrate periscopedb up
cbd migrate periscopedb pending

cbd migrate datalakedb up
cbd migrate datalakedb pending

cbd migrate freeipadb up
cbd migrate freeipadb pending

cbd migrate redbeamsdb up
cbd migrate redbeamsdb pending

cbd migrate environmentdb up
cbd migrate environmentdb pending

cbd migrate remoteenvironmentdb up
cbd migrate remoteenvironmentdb pending
```
You can track any other application's logs to check the results by executing the following command:
```
cbd logs periscope # or any other service name supported in CB_LOCAL_DEV_LIST
```

If everything went well then Cloudbreak will be available on https://localhost. For more details and config parameters please check the documentation of [Cloudbreak Deployer](https://github.infra.cloudera.com/cloudbreak/cloudbreak-deployer).

The deployer has generated a `certs` directory under `cbd-local` directory which will be needed later on to set up IDEA properly.

If not already present, you shall create an `etc` directory under `cbd-local` directory and place your Cloudera Manager license file `license.txt` there. This is essential for Thunderhead Mock to start successfully. (Request a licence from us)

### Update Latest From Source
CBD CLI supports update-to-dev (`cbd update-to-dev`) parameter which can be used to upgrade prebuild and published copy of CBD from build repo. However this will allow building from the Source Directly.

After successfully completing the CloudBreak Deployer instructions to successfully install CloudBreak this instruction can help build the latest Cloud Break Deployer from the Source code.
This activity may be recommended to pick the latest changes that are not published to the build repository.

pull the latest cloudbreak-deployer from github	https://github.infra.cloudera.com/cloudbreak/cloudbreak-deployer.git
change directory to the local workspace of the pulled source usually `cd cloudbreak-deployer` and following commands to build and copy the build output
```shell
make release-docker
cp build/Darwin/cbd `which cbd`
cbd --version   # to test the latest development version is installed. 
```
NOTE: release-docker will build the cloudbreak-deployer in a preconfigured docker container and hence this requires docker daemon to be running.

### Cloudbreak Service Ports

When `cloudbreak` is started in a container, the port it listens on is 8080.  If `cloudbreak` is added to the `CB_LOCAL_DEV_LIST` variable, all services expect the `cloudbreak` port to be 9091.

### Linux Difference

Cloudbreak Deployer is unable to determine the IP address on a Linux machine. Therefore, you must add in the public IP address manually to your `Profile`.

```
export PUBLIC_IP=127.0.0.1
```

## Enable gitconfig, githooks

We have some quality of life configurations and scripts that helps you submit a pull-request in a proper format.

Running the following command will enable them for you:
```
make enable-gitconfig
```
This will update the .git/config directory to apply configs from the .gitconfig file in the root of the project.

## IDEA

### Check Out the Cloudbreak Repository

Go to https://github.infra.cloudera.com/cloudbreak/cloudbreak, either clone or download the repository, use SSH which is described here: https://help.github.com/articles/connecting-to-github-with-ssh/

**Important**: update `~/.gradle/gradle.properties` file with the two following properties in order to download artifacts from the internal repository. You can find the details on our [Wiki page](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/793674498/CM+SDK+Credentials+-+Gradle+properties).
- `defaultCmPrivateRepoUser`
- `defaultCmPrivateRepoPassword`

### Project Settings in IDEA

In IDEA set your SDK to your Java version under:

Set project SDK
```
File -> Project Structure -> Project Settings -> Project -> Project SDK -> 21
```

Set project Language level
```
File -> Project Structure -> Project Settings -> Project -> Project Language Level -> 21
```

Set Gradle JVM
```
IntelliJ IDEA -> Settings -> Build, Execution, Deployment -> Gradle -> Gradle JVM -> 21
```

Set IDEA compiler JVM heap memory
```
File -> Settings -> Build, Execution, Deployment -> Compiler -> Shared heap size -> 1500
```

Set additional command line parameters
```
IntelliJ IDEA -> Settings -> Build, Execution, Deployment -> Compiler -> Java Compiler -> Additional command line parameters -> -parameters
```

Set Import Order
```
Import static all other imports
<blank line>
import java.*
<blank line>
import javax.*
<blank line>
import jakarta.*
<blank line>
import org.*
<blank line>
import com.*
<blank line>
import all other imports
```

### Import Project

Cloudbreak can be imported into IDEA as a Gradle project by specifying the `cloudbreak` repo root under Import Project. Once it is done, you need to import the proper code formatter by using the `File -> Import Settings...` menu and selecting the `idea_settings.jar` located in the `config/idea` directory in the Cloudbreak git repository.

Also, you need to import inspection settings called `inpsections.xml` located in `config/idea`:
```
IntelliJ IDEA -> Settings -> Editor -> Inspections -> Settings icon -> Import Profile
```

Cloudbreak integrates with GRPC components. This results in generated files inside the project with big file sizes. By default, IDEA ignores anything that is more than 8MB, resulting in unknown classes inside the IDEA context. To circumvent this, you need to add this property to your IDEA properties.

Go to `Help -> Edit Custom Properties...`, then insert
```
#parse files up until 25MB
idea.max.intellisense.filesize=25000
```
Restart IDEA, and Rebuild.

#### Activating Cloudbreak Code Styles
After importing, be sure to navigate to:
```text
IntelliJ IDEA -> Settings -> Editor -> Code Style -> Java -> Scheme
```
And, select the new scheme `Default (1)`.

Otherwise, IntelliJ will constantly reorder your imports differently from CB conventions.

### PKIX SSL Error - Import Mock-Infrastructure's certificate to your Java trust store before launch FreeIPA or Cloudbreak(core) locally
[We needed to eliminate](https://github.infra.cloudera.com/cloudbreak/cloudbreak/pull/13278/files#diff-f1e661d9b4ab2d595976d4ed701e8f29907fa1b4a6fac64214dd2549eabd6e40L62-L83) the vulnerable `TrustEveryThingTrustStore` implementation from our code base
and this indicates that the certificate of the Mock-Infrastructure service needs to be added to the Java trust store **if you run FreeIPA and/or Cloudbreak(core) services locally and would like to create deployments with `mock` provider or simply run the integration tests locally.**
Because the image catalog could not be downloaded from the mentioned service due to SSL handshake issues, for example:
```
{"message":"Creation of FreeIPA failed: Failed to get image catalog: javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target from https://localhost:10090/mock-image-catalog?catalog-name=freeipa-catalog&cb-version=2.68.0-b64&runtime=7.2.15&mock-server-address=localhost:10090","payload":null}
```

Example import commands, **do not forget to update the path of the certificate to your Cloudbreak repository location:**
```
# How to import on Linux
sudo keytool -import -alias mock-infra -noprompt -file "/home/${USER}/prj/cloudbreak/mock-infrastructure/src/main/resources/keystore/infrastructure-mock.cer" -keystore         /etc/ssl/certs/java/cacerts -storepass changeit

# How to import on MacOS
keytool -import -alias mock-infra -noprompt -file ~/prj/cloudbreak/cloudbreak/mock-infrastructure/src/main/resources/keystore/infrastructure-mock.cer -keystore /opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home/lib/security/cacerts -storepass changeit
```

If you still get the same error then specify the `Trust Store` for `Cloudbreak(core)` and/or `FreeIpa`:
```
# How to import on Linux
-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts

# How to import on MacOS
-Djavax.net.ssl.trustStore=/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home/lib/security/cacerts
```

***Note:***
- The path of the Java trust store may be different for your development environment, then please update path in the commands to the right location
- This solution is only needed until the [CB-18493](https://cloudera.atlassian.net/browse/CB-18493) has been solved.

### Running Cloudbreak in IDEA

To launch the Cloudbreak application execute the `com.sequenceiq.cloudbreak.CloudbreakApplication` class (set `Use classpath of module` to `cloudbreak.core.main`) with the following JVM options:
```
-Dcb.db.port.5432.tcp.addr=localhost
-Dcb.db.port.5432.tcp.port=5432
-Dserver.port=9091
-Daltus.ums.host=localhost
-Dvault.addr=localhost
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Dinstance.node.id=<NODE_ID>
```

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file and `<NODE_ID>` with some value e.g.: CB-1

Note that if you're upgrading from 2.16 (or earlier) to master you may have to set this value in the database also to ensure the flow restart functionality for progressing cluster(s)

You can set this by executing the following SQL on the cbdb database:

```
UPDATE flowlog 
SET cloudbreaknodeid = 'YOUR_NODE_ID_VALUE';
``` 
Where the `YOUR_NODE_ID_VALUE` value must be the same what you provide in the cloudbreak run configuration mentioned above.

Afterward add these entries to the environment variables (the same values that you set in `Profile`):
```
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
CB_AWS_ACCOUNT_ID=
AWS_GOV_ACCESS_KEY_ID=
AWS_GOV_SECRET_ACCESS_KEY=
CB_AWS_GOV_ACCOUNT_ID=
```

The database migration scripts are run automatically by Cloudbreak, but this migration can be turned off with the `-Dcb.schema.migration.auto=false` JVM option.

### Configure Before launch task

In order to be able to determine the local `Cloudbreak` and `FreeIPA` version automatically, a `Before launch` task has to be configured for the project in IntelliJ IDEA. The required steps are the following:

1. Open `Run/Debug Configurations` for the project
2. Select your project's application
3. Click on `Add` in the `Before launch` panel
4. Select `Run Gradle Task` with the following parameters
    1. `Gradle project`: `cloudbreak:core` or `cloudbreak:freeipa` depending on the service
    2. `Tasks`: `buildInfo`
5. Confirm and restart the application

### Running Periscope in IDEA

After importing the `cloudbreak` repo root, launch the Periscope application by executing the `com.sequenceiq.periscope.PeriscopeApplication` class (set `Use classpath of module` to `cloudbreak.autoscale.main`) with the following JVM options:
* Note: If `cloudbreak` is in the `CB_LOCAL_DEV_LIST` variable, the `periscope.cloudbreak.url` should be http://localhost:9091
```
-Dperiscope.db.port.5432.tcp.addr=localhost
-Dperiscope.db.port.5432.tcp.port=5432
-Dperiscope.cloudbreak.url=http://localhost:8080
-Dserver.port=8085
-Daltus.ums.host=localhost
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Dinstance.node.id=<NODE_ID>
--add-opens java.base/java.util.concurrent=ALL-UNNAMED
```

Replace `<VAULT_ROOT_TOKEN>` and `<NODE_ID>` with the value of `VAULT_ROOT_TOKEN` and `CB_INSTANCE_NODE_ID` respectively from the `Profile` file.

### Running Datalake in IDEA

After importing the `cloudbreak` repo root, launch the Datalake application by executing the `com.sequenceiq.datalake.DatalakeApplication` class (set `Use classpath of module` to `cloudbreak.datalake.main`) with the following JVM options:
* Note: If `cloudbreak` is in the `CB_LOCAL_DEV_LIST` variable, the `datalake.cloudbreak.url` should be http://localhost:9091
```
-Dserver.port=8086
-Dcb.enabledplatforms=AWS,AZURE,MOCK
-Ddatalake.cloudbreak.url=http://localhost:8080
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Dvault.addr=localhost
-Dinstance.node.id=<NODE_ID>
```

Replace `<VAULT_ROOT_TOKEN>` and `<NODE_ID>` with the value of `VAULT_ROOT_TOKEN` and `CB_INSTANCE_NODE_ID` respectively from the `Profile` file.

### Running FreeIPA in IDEA

After importing the `cloudbreak` repo root, launch the FreeIPA application by executing the `com.sequenceiq.freeipa.FreeIpaApplication` class (set `Use classpath of module` to `cloudbreak.freeipa.main`) with the following JVM options:

```
-Dfreeipa.db.addr=localhost
-Dserver.port=8090
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Dinstance.node.id=<NODE_ID>
```

Replace `<VAULT_ROOT_TOKEN>` and `<NODE_ID>` with the value of `VAULT_ROOT_TOKEN` and `CB_INSTANCE_NODE_ID` respectively from the `Profile` file.

Then add these entries to the environment variables (the same values that you set in `Profile`):
```
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
CB_AWS_ACCOUNT_ID=
AWS_GOV_ACCESS_KEY_ID=
AWS_GOV_SECRET_ACCESS_KEY=
CB_AWS_GOV_ACCOUNT_ID=
```

### Running Redbeams in IDEA

After importing the `cloudbreak` repo root, launch the Redbeams application by executing the `com.sequenceiq.redbeams.RedbeamsApplication` class (set `Use classpath of module` to `cloudbreak.redbeams.main`) with the following JVM options:
* Note: If `cloudbreak` is in the `CB_LOCAL_DEV_LIST` variable, the `redbeams.cloudbreak.url` should be http://localhost:9091
```
-Dredbeams.db.port.5432.tcp.addr=localhost
-Dredbeams.db.port.5432.tcp.port=5432
-Dredbeams.cloudbreak.url=http://localhost:8080
-Dserver.port=8087
-Daltus.ums.host=localhost
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Dcb.enabledplatforms=AWS,AZURE,MOCK
-Dinstance.node.id=<NODE_ID>
```

Replace `<VAULT_ROOT_TOKEN>` and `<NODE_ID>` with the value of `VAULT_ROOT_TOKEN` and `CB_INSTANCE_NODE_ID` respectively from the `Profile` file.

Then add these entries to the environment variables (the same values that you set in `Profile`):
```
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
CB_AWS_ACCOUNT_ID=
AWS_GOV_ACCESS_KEY_ID=
AWS_GOV_SECRET_ACCESS_KEY=
CB_AWS_GOV_ACCOUNT_ID=
```

### Running the Environment Service in IDEA

After importing the `cloudbreak` repo root, launch the Environment application by executing the `com.sequenceiq.environment.EnvironmentApplication` class (set `Use classpath of module` to `cloudbreak.environment.main`) with the following JVM options:
* Note: If `cloudbreak` is in the `CB_LOCAL_DEV_LIST` variable, the `environment.cloudbreak.url` should be http://localhost:9091
```
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Denvironment.cloudbreak.url=http://localhost:8080
-Denvironment.enabledplatforms="YARN,YCLOUD,AWS,AZURE,MOCK"
-Dinstance.node.id=<NODE_ID>
```

Replace `<VAULT_ROOT_TOKEN>` and `<NODE_ID>` with the value of `VAULT_ROOT_TOKEN` and `CB_INSTANCE_NODE_ID` respectively from the `Profile` file.

Then add these entries to the environment variables (the same values that you set in `Profile`):
```
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
CB_AWS_ACCOUNT_ID=
AWS_GOV_ACCESS_KEY_ID=
AWS_GOV_SECRET_ACCESS_KEY=
CB_AWS_GOV_ACCOUNT_ID=
```

### Running the Remote Environment Service in IDEA

After importing the `cloudbreak` repo root, launch the Remote Environment application by executing the `com.sequenceiq.remoteenvironment.RemoteEnvironmentApplication` class (set `Use classpath of module` to `cloudbreak.remoteenvironment.main`) with the following JVM options. 
Please do not forget to replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` respectively from the `Profile` file.

#### JVM options for local development setup with mocked cluster-proxy and remote-cluster service endpoints
```
-Dremoteenvironment.identity.server.url=http://localhost:8092
-Dserver.port=8092
-DclusterProxy.url=http://localhost:8080/cluster-proxy/
-Dvault.root.token=<VAULT_ROOT_TOKEN>
```



#### JVM options for local development setup with cluster-proxy and remote-cluster services port-forwarded from Manowar-dev environment
```
-Dremoteenvironment.identity.server.url=http://localhost:8092
-Dserver.port=8092
-Daltus.ums.host=ums.thunderhead-dev.cloudera.com
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Dremotecluster.host=localhost
-Dremotecluster.port=9983
-DclusterProxy.url=http://localhost:10082/cluster-proxy
```

You need remote cluster service from thunderhead with:
```
kubectl port-forward deployment/thunderhead-remotecluster 9983:8982 --namespace thunderhead-remotecluster
```

Also if you want to test the e2e flow then you need cluster proxy with:
```
kubectl -n cluster-proxy port-forward svc/cluster-proxy-cluster-proxy 10082:10080
```

#### Example cURL commands
You can register a control with:
```
curl -X POST -H "Content-Type: application/json" \
-d '{"items":[{"crn":"<CRN example crn:cdp:hybrid:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:pvcControlPlane:b3654f5f-bd9c-4399-a4e5-e1628970712b>","url":"http://test.com","name":"test"}]}' \
-H "x-cdp-actor-crn: crn:cdp:iam:us-west-1:altus:user:__internal__actor__" \
http://localhost:8089/remoteenvironmentservice/api/v1/control_plane
```

You can list environments with:
```
curl -X GET -H "Content-Type: application/json" \
-H "x-cdp-actor-crn: crn:cdp:iam:us-west-1:hortonworks:user:user@hortonworks.com" \
http://localhost:8089/remoteenvironmentservice/api/v1/env
```

You can describe environment with:
```
curl -X GET -H "Content-Type: application/json" \
-H "x-cdp-actor-crn: crn:cdp:iam:us-west-1:hortonworks:user:user@hortonworks.com" \
http://localhost:8089/remoteenvironmentservice/api/v1/env/crn/crn:altus:environments:us-west-1:5abe6882-ff63-4ad2-af86-a5582872a9cd:environment:test-hybrid-1/06533e78-b2bd-41c9-8ac4-c4109af7797b
```

### Running the Externalized Compute Service in IDEA

After importing the `cloudbreak` repo root, launch the Externalized Compute application by executing the `com.sequenceiq.externalizedcompute.ExternalizedComputeClusterApplication` class (set `Use classpath of module` to `cloudbreak.externalized-compute.main`) with the following JVM options:

```
--add-opens java.base/java.lang=ALL-UNNAMED
-Dserver.port=8091
-Dinstance.node.id=<NODE_ID>
```

Replace `<NODE_ID>` with the value of `CB_INSTANCE_NODE_ID` respectively from the `Profile` file.

### Running Thunderhead Mock in IDEA

After importing the `cloudbreak` repo root, launch the Thunderhead Mock application by executing the `com.sequenceiq.thunderhead.MockThunderheadApplication` class (set `Use classpath of module` to `cloudbreak.mock-thunderhead.main`) with the following JVM options:

```
-Dauth.config.dir=<CBD_LOCAL_ETC>
```

Replace `<CBD_LOCAL_ETC>` with the full path of your `cbd-local/etc` directory that shall already contain the Cloudera Manager license file `license.txt`.

Please make sure that `thunderhead-api` has also been added to `CB_LOCAL_DEV_LIST` list in `Profile` file of cbd (besides `thunderhead-mock`).

### Running Mock-Infrastructure in IDEA

After importing the `cloudbreak` repo root, launch the mock-infrastructure application by executing the `com.sequenceiq.mock.MockInfrastructureApplication` class (set `Use classpath of module` to `cloudbreak.mock-infrastructure.main`) with the following JVM options:
```
--add-opens java.base/java.util=ALL-UNNAMED
```


Please make sure that `mock-infrastructure` has been added to `CB_LOCAL_DEV_LIST` list in the `Profile` file of cbd.

In the `Profile` file make sure to also add:

```
export MOCK_INFRASTRUCTURE_HOST=localhost
```

### Virtual thread support

Cloudbreak, Freeipa, DataLake, Environment, Redbeams services can be started with virtual threads. For this the services need to be started with `-Dspring.threads.virtual.enabled=true` jvm argument.

Setting the `spring.threads.virtual.enabled` to `true` configures:

* tomcat thread pool,
* flow executor thread pool,
* quartz thread pool,
* azure client thread pool,
* user sync thread pools, 
* resource builder and intermediate thread pools

to use virtual threads.

## Command Line

### Running Cloudbreak from the Command Line
To run Cloudbreak from the command line first set the AWS environment variables (use the same values as in `Profile`)

```
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export CB_AWS_ACCOUNT_ID=...
export AWS_GOV_ACCESS_KEY_ID=...
export AWS_GOV_SECRET_ACCESS_KEY=...
export CB_AWS_GOV_ACCOUNT_ID=...
```

Set the CM repository credentials in order to download artifacts from the internal repository. Ask us for details in the [#eng_cb_dev_internal](https://cloudera.slack.com/archives/CF66M7WP6) Slack channel. 
```
export CM_PRIVATE_REPO_USER=
export CM_PRIVATE_REPO_PASSWORD=
```

then run the following Gradle command:

```
./gradlew :core:buildInfo :core:bootRun --no-daemon -PjvmArgs="-Dcb.db.port.5432.tcp.addr=localhost \
-Dcb.db.port.5432.tcp.port=5432 \
-Dcb.schema.scripts.location=$(pwd)/core/src/main/resources/schema
-Dserver.port=9091 \
-Daltus.ums.host=localhost
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Dspring.config.location=$(pwd)/core/src/main/resources/application.yml,$(pwd)/core/src/main/resources/application-dev.yml,$(pwd)/core/build/resources/main/application.properties"
```

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.


The database migration scripts are run automatically by Cloudbreak, this migration can be turned off with the `-Dcb.schema.migration.auto=false` JVM option.

### Running Periscope from the Command Line
To run Periscope from the command line, run the following Gradle command:
* Note: If `cloudbreak` is in the `CB_LOCAL_DEV_LIST` variable, the `periscope.cloudbreak.url` should be http://localhost:9091
````
./gradlew :autoscale:bootRun -PjvmArgs="-Dperiscope.db.port.5432.tcp.addr=localhost \
-Dperiscope.db.port.5432.tcp.port=5432 \
-Dperiscope.cloudbreak.url=http://localhost:8080 \
-Dperiscope.schema.scripts.location=$(pwd)/autoscale/src/main/resources/schema
-Dserver.port=8085 \
-Daltus.ums.host=localhost \
-Dvault.root.token=<VAULT_ROOT_TOKEN> \
-Dspring.config.location=$(pwd)/autoscale/src/main/resources/application.yml,$(pwd)/autoscale/build/resources/main/application.properties"
````

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.

### Running Datalake from the Command Line
To run Datalake from the command line, run the following Gradle command:
* Note: If `cloudbreak` is in the `CB_LOCAL_DEV_LIST` variable, the `datalake.cloudbreak.url` should be http://localhost:9091
````
./gradlew :datalake:bootRun -PjvmArgs="-Dvault.root.token=<VAULT_ROOT_TOKEN> \
-Dserver.port=8086 \
-Ddatalake.cloudbreak.url=http://localhost:8080
-Dspring.config.location=$(pwd)/datalake/src/main/resources/application.yml,$(pwd)/datalake/build/resources/main/application.properties"
````

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.

### Running FreeIPA from the Command Line
To run the FreeIPA management service from the command line first set the AWS environment variables (use the same values as in `Profile`)

```
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export CB_AWS_ACCOUNT_ID=...
export AWS_GOV_ACCESS_KEY_ID=...
export AWS_GOV_SECRET_ACCESS_KEY=...
export CB_AWS_GOV_ACCOUNT_ID=...
```

then run the following Gradle command:

````
./gradlew :freeipa:bootRun --no-daemon -PjvmArgs="-Dfreeipa.db.addr=localhost \
-Dserver.port=8090 \
-Dvault.root.token=<VAULT_ROOT_TOKEN> \
-Dspring.config.location=$(pwd)/freeipa/src/main/resources/application.yml,$(pwd)/freeipa/src/main/resources/application-dev.yml,$(pwd)/freeipa/build/resources/main/application.properties"
````

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.

### Running Redbeams from the Command Line
To run the Redbeams from the command line first set the AWS environment variables (use the same values as in `Profile`)

```
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export CB_AWS_ACCOUNT_ID=...
export AWS_GOV_ACCESS_KEY_ID=...
export AWS_GOV_SECRET_ACCESS_KEY=...
export CB_AWS_GOV_ACCOUNT_ID=...
```

then run the following Gradle command:
* Note: If `cloudbreak` is in the `CB_LOCAL_DEV_LIST` variable, the `redbeams.cloudbreak.url` should be http://localhost:9091
```
./gradlew :redbeams:bootRun --no-daemon -PjvmArgs="-Dredbeams.db.port.5432.tcp.addr=localhost \
-Dredbeams.db.port.5432.tcp.port=5432 \
-Dredbeams.cloudbreak.url=http://localhost:8080 \
-Dredbeams.schema.scripts.location=$(pwd)/redbeams/src/main/resources/schema \
-Dserver.port=8087 \
-Daltus.ums.host=localhost \
-Dvault.root.token=<VAULT_ROOT_TOKEN> \
-Dspring.config.location=$(pwd)/redbeams/src/main/resources/application.yml,$(pwd)/redbeams/build/resources/main/application.properties"
```

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.

### Running the Environment Service from the Command Line
To run the Environment service from the command line first set the AWS environment variables (use the same values as in `Profile`)

```
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export CB_AWS_ACCOUNT_ID=...
export AWS_GOV_ACCESS_KEY_ID=...
export AWS_GOV_SECRET_ACCESS_KEY=...
export CB_AWS_GOV_ACCOUNT_ID=...
```

then run the following Gradle command:
* Note: If `cloudbreak` is in the `CB_LOCAL_DEV_LIST` variable, the `environment.cloudbreak.url` should be http://localhost:9091
```
./gradlew :environment:bootRun -PjvmArgs="\
-Denvironment.cloudbreak.url=http://localhost:8080 \
-Dvault.root.token=<VAULT_ROOT_TOKEN> \
-Dspring.config.location=$(pwd)/environment/src/main/resources/application.yml,$(pwd)/environment/build/resources/main/application.properties"
```

### Running the Remote Environment Service from the Command Line
To run the Remote Environment service from the command line:

```
./gradlew :environment-remote:bootRun -PjvmArgs="\
-Dremoteenvironment.cloudbreak.url=http://localhost:8080 \
-Dvault.root.token=<VAULT_ROOT_TOKEN> \
-Dspring.config.location=$(pwd)/remoteenvironment/src/main/resources/application.yml,$(pwd)/remoteenvironment/build/resources/main/application.properties"
```

### Running the Externalized Compute Service from the Command Line
To run the Externalized Compute service from the command line run the following Gradle command:

```
./gradlew :externalized-compute:bootRun -PjvmArgs="\
--add-opens java.base/java.lang=ALL-UNNAMED \
-Dserver.port=8091 \
-Dinstance.node.id=<NODE_ID> \
-Dspring.config.location=$(pwd)/externalized-compute/src/main/resources/application.yaml"
```

### Running Thunderhead Mock from the Command Line
To run Thunderhead Mock from the command line, run the following Gradle command:

```
./gradlew :mock-thunderhead:bootRun -PjvmArgs="\
-Dserver.port=10080 \
-Dauth.config.dir=<CBD_LOCAL_ETC> \
-Dspring.config.location=$(pwd)/mock-thunderhead/src/main/resources/application.yml"
```

Replace `<CBD_LOCAL_ETC>` with the full path of your `cbd-local/etc` directory that shall already contain the Cloudera Manager license file `license.txt`.

Please make sure that `thunderhead-api` has also been added to `CB_LOCAL_DEV_LIST` list in `Profile` file of cbd (besides `thunderhead-mock`).

### Running Mock-Infrastructure from the Command Line
To run Mock-Infrastructure from the command line first set the following environment variable

```
export MOCK_INFRASTRUCTURE_HOST=localhost
```

then run the following Gradle command:
```
./gradlew :mock-infrastructure:bootRun -PjvmArgs="\
--add-opens java.base/java.util=ALL-UNNAMED \
-Dspring.config.location=$(pwd)/mock-infrastructure/src/main/resources/application.yml"
```

Please make sure that `mock-infrastructure` has been added to `CB_LOCAL_DEV_LIST` list in the `Profile` file of cbd.

## Database Development

If any schema change is required in Cloudbreak services databases (`cbdb` / `periscopedb` / `datalakedb` / `redbeamsdb` / `environmentdb` / `freeipadb`/ `remoteenvironmentdb`), then the developer needs to write SQL scripts
 to migrate the database accordingly. The schema migration is managed by [MyBatis Migrations](https://github.com/mybatis/migrations) in Cloudbreak and the cbd tool provides an easy-to-use wrapper for it. The syntax for using the migration commands is `cbd migrate <database name> <command> [parameters]` e.g. `cbd migrate cbdb status`.
Create a SQL template for schema changes:
```
cbd migrate cbdb new "CLOUD-123 schema change for new feature"
```
As result of the above command an SQL file template is generated under the path specified in `CB_SCHEMA_SCRIPTS_LOCATION` environment variable, which is defined in `Profile`. The structure of the generated SQL template looks like the following:
```
-- // CLOUD-123 schema change for new feature
-- Migration SQL that makes the change goes here.



-- //@UNDO
-- SQL to undo the change goes here.
```
Once you have implemented your SQLs then you can execute them with:
```
cbd migrate <database-name> up
```
Make sure pending SQLs to run as well:
```
cbd migrate <database-name> pending
```
If you would like to roll back the last SQL file, then just use the down command:
```
cbd migrate <database-name> down
```
In order to check the status of database
```
cbd migrate <database-name> status

#Every script that has not been executed will be marked as ...pending... in the output of status command:

------------------------------------------------------------------------
-- MyBatis Migrations - status
------------------------------------------------------------------------
ID             Applied At          Description
================================================================================
20150421140021 2015-07-08 10:04:28 create changelog
20150421150000 2015-07-08 10:04:28 CLOUD-607 create baseline schema
20150507121756 2015-07-08 10:04:28 CLOUD-576 change instancegrouptype hostgroup to core
20151008090632    ...pending...    CLOUD-123 schema change for new feature

------------------------------------------------------------------------
```

## Building

Gradle is used for build and dependency management. The Gradle wrapper is added to the Cloudbreak git repository, so building can be done with:
```
./gradlew clean build
```

Before running the above command, however, be sure to make the changes mentioned in [Check Out the Cloudbreak Repository](#check-out-the-cloudbreak-repository) to your `~/.gradle/gradle.properties`.

## How to Reach CM UI Directly (Not Through Knox)
With the current design on the cluster's gateway node there is an NGiNX which is responsible for routing requests through Knox by default. 
But there are cases when the CM UI needs to be reached directly. It is possible on the same port by the same NGiNX on the `clouderamanager/` path of the provisioned cluster.
Please note that the trailing slash is significant for the routing to work.

For example: `https://tb-nt-local.tb-local.xcu2-8y8x.workload-dev.cloudera.com/clouderamanager/`

> Be aware of that this routing mechanism is based on cookies, so if you have problems to reach the CM UI directly especially when you reached any service through Knox previously then the deletion of cookies could solve your issues.

# Api definitions

We are using Openapi to create API definitions for our services. The following list contains all the service and the specific link for the definitions.

|      Service      |                                     Openapi JSON                                      | 
|:-----------------:|:-------------------------------------------------------------------------------------:|
|    Cloudbreak     |    https://cloudbreak-swagger.s3.eu-central-1.amazonaws.com/openapi-<VERSION>.json    |
|      FreeIPA      |       https://freeipa-swagger.s3.us-east-2.amazonaws.com/openapi-<VERSION>.json       |
|    Environment    |     https://environment-swagger.s3.us-east-2.amazonaws.com/openapi-<VERSION>.json     |
|     DataLake      |      https://datalake-swagger.s3.us-east-2.amazonaws.com/openapi-<VERSION>.json       |
|     RedBeams      |      https://redbeams-swagger.s3.us-east-2.amazonaws.com/openapi-<VERSION>.json       |
|     Periscope     |      https://autoscale-swagger.s3.us-east-2.amazonaws.com/openapi-<VERSION>.json      |
| RemoteEnvironment |  https://remoteenvironment-swagger.s3.us-east-2.amazonaws.com/openapi-<VERSION>.json  |
|  ExternalCompute  | https://externalizedcompute-swagger.s3.us-east-2.amazonaws.com/openapi-<VERSION>.json |

# How to Contribute

I would like to start by the warm welcome if you would like to contribute to our project, making our - and from the point of contribution, it's yours also - goals closer. 

We're happy for having your helpful intention to make this project greater than ever, but for this, I'd like to introduce you to some of our guidelines that you should follow for a successful contribution.

When you would like to make a contribution you can do that by opening pull request against the desired version, but along with some very suggested guidelines not just for the sake of understandability but for having a properly combined request.


## Appearance

First, let's start with the appearance.
At the time of this writing, we don't enforce any formal requirements to the pull request message by any kind of tool, but we have the following strongly recommended guidelines:

- if your commit message/jira description fits into a Twitter message then probably, it is too short, and might not be clear what is the intention there
- if it contains words like: fix or handle then probably you should consider some rewording, but of course, sometimes it is acceptable
- if your commit fixes something obvious, e.g, a compile error, then of course you don’t need to write a long description about why it is a good idea to fix a compile error
- compared to a 200-1000 lines of code change (which is the size of our average commit), adding a few more lines into the commit message/jira description is just a tiny effort but would make a huge difference

We were talking about what we should avoid, but let's see a few good examples, which helps the reviewer to understand the purpose of that commit:

https://github.infra.cloudera.com/cloudbreak/cloudbreak/commit/56fdde5c6f48f48a378b505a170b3e3d83225c85

https://github.infra.cloudera.com/cloudbreak/cloudbreak/commit/d09b0074c45af209ccf34855dcf4c1f34c3ccebb

https://github.infra.cloudera.com/cloudbreak/cloudbreak/commit/c93b91fd6a08de7516ab763098f2dcd3abc149f0

https://github.infra.cloudera.com/cloudbreak/cloudbreak/commit/f50f5c8f38941db958eac27c663ae00ecba7b0f5


## Coding Guidelines

- If you introduce a new Cloud SDK or API for a feature, please ensure that the newly introduced API calls **are supported in every region** and if not then search for an alternative solution. It is often the case that the cloud providers gradually introduce their new services.

- Use strings instead of enums on APIs for better backward compatibility - enums are useful for the service layer but on the API layer each new value can break compatibility.

- API guidelines:
  - New `POST`, `PUT` and `DELETE` endpoints should have the request body marked with `@RequestBody(required = true)`, if the request doesn't make sense without a request body
      - Note, that there are two `@RequestBody` annotations. You need to use the one from `io.swagger.v3.oas.annotations.parameters`
  - Prefer using primitive types over their wrapper classes wherever possible, and mark them required in response classes (and even in request classes if it makes sense)
      - a field can be marked required, by adding the following annotation to it: `@Schema(requiredMode = Schema.RequiredMode.REQUIRED)`
  - Strongly prefer `boolean` over `Boolean`, and if you must use the wrapper type, give it a default value
  - For fields extending `Collection<T>` in response classes, give them a default value of an empty collection and mark them required
      - when giving a default value to a collection, prefer using mutable collections instead of constructs like `List.of()` or `Map.of()`
  - In general, mark any field you deem essential required, and give it a default value if possible

## Catching Up

When you're working on your precious change on your beloved branch and all of a sudden you face the issue of getting your branch drop behind from the desired/initial branch where you would like to open your future pull request, our way of catching up is [rebasing](https://git-scm.com/docs/git-rebase).

If you're experiencing this quite common then the good practice would be fetching and rebasing the initial branch multiple times in a day because there are periods of times when dozens of changes are landing on different branches. I agree, sometimes (especially when someone is working on a huge change) continuously rebasing our branch could be could really be a pain in the bottom, but this practice ensures that we're submitting our commits in proper order and way.

In addition, please do not merge branches together if you can solve your problem with rebasing, and even if you think that your change would have no impact on the codebase, or the actual collection of functionalities **- if you're not from our team or don't have written permission from one of our team members - please never ever push directly anything to the master branch normally and especially not by force.** 


## Additional Info
> * [Cloudbreak Service Provider Interface (SPI)](https://github.infra.cloudera.com/cloudbreak/cloudbreak/blob/master/docs/spi.md)
