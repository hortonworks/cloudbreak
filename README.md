# Cloudbreak

[![CircleCI](https://circleci.com/gh/hortonworks/cloudbreak.svg?style=svg)](https://circleci.com/gh/hortonworks/cloudbreak)
[![Maintainability](https://api.codeclimate.com/v1/badges/566493a63aaaf0c61bd4/maintainability)](https://codeclimate.com/github/hortonworks/cloudbreak/maintainability)
[![Build Automated](https://img.shields.io/docker/automated/hortonworks/cloudbreak.svg)](https://hub.docker.com/r/hortonworks/cloudbreak/)
[![Build Pulls](https://img.shields.io/docker/pulls/hortonworks/cloudbreak.svg)](https://hub.docker.com/r/hortonworks/cloudbreak/)
[![Licence](https://img.shields.io/github/license/hortonworks/cloudbreak.svg)](https://github.com/hortonworks/cloudbreak/blob/fix-readme/LICENSE)

* Website: https://hortonworks.com/open-source/cloudbreak/
* Documentation: https://docs.hortonworks.com/HDPDocuments/Cloudbreak/Cloudbreak-2.7.0/index.html

# Local Development Setup
As of now this document focuses on setting up your development environment on macOS. You'll need Homebrew to install certain components in case you don't have them already. To get Homebrew please follow the install instructions on the Homebrew homepage: https://brew.sh

As a prerequisite, you need to have Java 11 installed. You can choose from many options, including the Oracle JDK, Oracle OpenJDK, or an OpenJDK from any of several providers. For help in choosing your JDK, consult [Java is Still Free](https://medium.com/@javachampions/java-is-still-free-2-0-0-6b9aa8d6d244).

You'll need Docker. For Mac, use [Docker Desktop for Mac](https://docs.docker.com/docker-for-mac/install/).

## Cloudbreak Deployer

The simplest way to setup the working environment to be able to start Cloudbreak on your local machine is to use the [Cloudbreak Deployer](https://github.com/hortonworks/cloudbreak-deployer).

First you need to create a sandbox directory which will store the necessary configuration files and dependencies of [Cloudbreak Deployer](https://github.com/hortonworks/cloudbreak-deployer). This directory must be created outside of the cloned Cloudbreak git repository:
```
mkdir cbd-local
cd cbd-local
```

The next step is to download the latest cloudbreak-deployer onto your machine:
```
curl -s https://raw.githubusercontent.com/hortonworks/cloudbreak-deployer/master/install-dev | sh && cbd --version
```
Add the following to the file named `Profile` under the cbd-local directory you have just created. Please note, when a `cbd` command is executed you should go to the deployment's directory where your `Profile` file is found (`cbd-local` in our example). The CB_SCHEMA_SCRIPTS_LOCATION environment variable configures the location of SQL scripts that are in the 'core/src/main/resources/schema' directory in the cloned Cloudbreak git repository.

Please note that the full path needs to be configured and env variables like $USER cannot be used. You also have to set a password for your local Cloudbreak in UAA_DEFAULT_USER_PW:

```
export ULU_SUBSCRIBE_TO_NOTIFICATIONS=true
export CB_INSTANCE_UUID=$(uuidgen | tr '[:upper:]' '[:lower:]')
export CB_SCHEMA_SCRIPTS_LOCATION=/Users/YOUR_USERNAME/YOUR_PROJECT_DIR/cloudbreak/core/src/main/resources/schema
export ENVIRONMENT_SCHEMA_SCRIPTS_LOCATION=/Users/YOUR_USERNAME/YOUR_PROJECT_DIR/cloudbreak/environment/src/main/resources/schema
export UAA_DEFAULT_USER_PW=YOUR_PASSWORD
export VAULT_AUTO_UNSEAL=true
```

In order to run Cloudbreak, Periscope, Datalake, FreeIPA, Redbeams, and Environment locally (from IDEA or the command line), put this into your Profile:
```
export CB_LOCAL_DEV_LIST=cloudbreak,periscope,datalake,freeipa,redbeams,environment
```

Containers for these applications won't be started and Uluwatu will connect to Java processes running on your host.
You don't have to put all of the applications into local-dev mode; the value of the variable could be any combination.

Then run these commands:
```
cbd start
cbd logs cloudbreak
```

In case you see org.apache.ibatis.migration.MigrationException at the end of the logs run these commands to fix the DB and the re-run the previous section(cbd start and logs):
```
cbd migrate cbdb up
cbd migrate cbdb pending
```

For some reason if you encounter a similar problem with Periscope, Datalake, Redbeams, or Environment, then run the following commands and you can restart the Cloudbreak Deployer:
```
cbd migrate periscopedb up
cbd migrate periscopedb pending

cbd migrate datalakedb up
cbd migrate datalakedb pending

cbd migrate redbeamsdb up
cbd migrate redbeamsdb pending

cbd migrate environmentdb up
cbd migrate environmentdb pending
```
You can track any other application's logs to check the results by executing the following command:
```
cbd logs periscope # or datalake, redbeams, environment
```

If everything went well then Cloudbreak will be available on https://localhost. For more details and config parameters please check the documentation of [Cloudbreak Deployer](https://github.com/hortonworks/cloudbreak-deployer).

The deployer has generated a `certs` directory under `cbd-local` directory which will be needed later on to set up IDEA properly.

### Linux difference

cbd is unable to determine the IP address on a Linux machine. Therefore, you must add in the public IP address manually to your Profile.

```
export PUBLIC_IP=127.0.0.1
```

## IDEA

### Check out the Cloudbreak repository

Go to https://github.com/hortonworks/cloudbreak, Either clone or download the repository, use SSH which is described here: https://help.github.com/articles/connecting-to-github-with-ssh/

### Project settings in IDEA

In IDEA set your SDK to your Java version under:

Set project SDK
```
File -> Project Structure -> Project Settings -> Project -> Project SDK -> 11
```

Set project Language level
```
File -> Project Structure -> Project Settings -> Project -> Project Language Level -> 11
```

Set Gradle JVM
```
IntelliJ IDEA -> Preferences -> Build, Execution, Deployment -> Gradle -> Gradle JVM -> 11
```

### Import project

Cloudbreak can be imported into IDEA as a Gradle project by specifying the cloudbreak repo root under Import Project. Once it is done, you need to import the proper code formatter by using the __File -> Import Settings...__ menu and selecting the `idea_settings.jar` located in the `config/idea` directory in the Cloudbreak git repository.

Also you need to import inspection settings called `inpsections.xml` located in `config/idea`:
```
IntelliJ IDEA -> Preferences -> Editor -> Inspections -> Settings icon -> Import Profile
```

Cloudbreak integrates with GRPC components. This results in generated files inside the project with big file sizes. By default IDEA ignores anything that is more than 8MB, resulting in unknown classes inside the IDEA context. To circumvent this, you need to add this property to your IDEA properties.

Go to: Help/Edit Custom Properties...
```
#parse files up until 15MB
idea.max.intellisense.filesize=15000
```
Restart IDEA, and Rebuild.

### Running cloudbreak in IDEA

To launch the Cloudbreak application execute the `com.sequenceiq.cloudbreak.CloudbreakApplication` class (set 'Use classpath of module' to `core_main`) with the following VM options:
```
-Dcb.client.id=cloudbreak
-Dcb.client.secret=<UAA_DEFAULT_SECRET>
-Dcb.db.port.5432.tcp.addr=localhost
-Dcb.db.port.5432.tcp.port=5432
-Dcb.identity.server.url=http://localhost:8089
-Dspring.cloud.consul.host=YOUR_IP
-Dserver.port=9091
-Daltus.ums.host=localhost
-Dvault.addr=localhost
-Dvault.root.token=<VAULT_ROOT_TOKEN>
```

Replace `<UAA_DEFAULT_SECRET>` with the value of UAA_DEFAULT_SECRET from the cdb-local/Profile file. Replace `<VAULT_ROOT_TOKEN>` with the value of VAULT_ROOT_TOKEN from the cbd-local/Profile file.

The database migration scripts are run automatically by Cloudbreak, but this migration can be turned off with the `-Dcb.schema.migration.auto=false` JVM option.

### Configure Before launch task

In order to be able to determine the local Cloudbreak version automatically, a `Before launch` task has to be configured for the project in IntelliJ IDEA. The required steps are the following:

1. Open `Run/Debug Configurations` for the project
2. Select your project's application
3. Click on `Add` in the `Before launch` panel
4. Select `Run Gradle Task` with the following parameters
    1. `Gradle project`: cloudbreak:core
    2. `Tasks`: buildInfo
5. Confirm and restart the application

### Running Periscope in IDEA

After importing the cloudbreak repo root, launch the Periscope application by executing the com.sequenceiq.periscope.PeriscopeApplication class with the following JVM options:

````
-Dperiscope.client.id=periscope
-Dperiscope.client.secret=<UAA_DEFAULT_SECRET>
-Dperiscope.identity.server.url=http://localhost:8089
-Dperiscope.db.port.5432.tcp.addr=localhost
-Dperiscope.db.port.5432.tcp.port=5432
-Dperiscope.cloudbreak.url=http://localhost:8080
-Dserver.port=8085
-Daltus.ums.host=localhost
-Dvault.addr=localhost
-Dvault.root.token=<VAULT_ROOT_TOKEN>
````

Replace `<UAA_DEFAULT_SECRET>` with the value of UAA_DEFAULT_SECRET from the cdb-local/Profile file. Replace `<VAULT_ROOT_TOKEN>` with the value of VAULT_ROOT_TOKEN from the cbd-local/Profile file.

### Running Datalake in IDEA

After importing the cloudbreak repo root, launch the Datalake application by executing the com.sequenceiq.datalake.DatalakeApplication class with the following JVM options:

````
-Dserver.port=8086
-Ddatalake.db.env.address=localhost
-Ddatalake.cloudbreak.url=http://localhost:8080
-Dvault.addr=localhost
-Dvault.root.token=<VAULT_ROOT_TOKEN>
````

### Running Redbeams in IDEA

After importing the cloudbreak repo root, launch the Redbeams application by executing the com.sequenceiq.redbeams.RedbeamsApplication class with the following JVM options:

```
-Dredbeams.client.id=redbeams
-Dredbeams.client.secret=<UAA_DEFAULT_SECRET>
-Dredbeams.identity.server.url=http://localhost:8089
-Dredbeams.db.port.5432.tcp.addr=localhost
-Dredbeams.db.port.5432.tcp.port=5432
-Dredbeams.cloudbreak.url=http://localhost:8080
-Dserver.port=8087
-Daltus.ums.host=localhost
-Dvault.addr=localhost
-Dvault.root.token=<VAULT_ROOT_TOKEN>
```

Replace `<UAA_DEFAULT_SECRET>` with the value of UAA_DEFAULT_SECRET from the cdb-local/Profile file. Replace `<VAULT_ROOT_TOKEN>` with the value of VAULT_ROOT_TOKEN from the cbd-local/Profile file.

### Running the Environment service in IDEA

After importing the cloudbreak repo root, launch the Environment application by executing the com.sequenceiq.environment.EnvironmentApplication class with the following JVM options:

```
-Dvault.root.token=<VAULT_ROOT_TOKEN>
```

Replace `<VAULT_ROOT_TOKEN>` with the value of VAULT_ROOT_TOKEN from the cbd-local/Profile file.

## Command line

### Cloudbreak
To run Cloudbreak from the command line, run the following Gradle command:

```
./gradlew :core:buildInfo :core:bootRun -PjvmArgs="-Dcb.client.id=cloudbreak \
-Dcb.client.secret=<UAA_DEFAULT_SECRET> \
-Dcb.db.port.5432.tcp.addr=localhost \
-Dcb.db.port.5432.tcp.port=5432 \
-Dcb.identity.server.url=http://localhost:8089 \
-Dcb.schema.scripts.location=$(pwd)/core/src/main/resources/schema
-Dserver.port=9091 \
-Daltus.ums.host=localhost
-Dvault.addr=localhost
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Dspring.config.location=$(pwd)/cloud-common/src/main/resources/application.yml,$(pwd)/core/build/resources/main/application.properties"
```

Replace `<UAA_DEFAULT_SECRET>` with the value of UAA_DEFAULT_SECRET from the cdb-local/Profile file. Replace `<VAULT_ROOT_TOKEN>` with the value of VAULT_ROOT_TOKEN from the cbd-local/Profile file.

The database migration scripts are run automatically by Cloudbreak, this migration can be turned off with the `-Dcb.schema.migration.auto=false` VM option.

### Periscope
To run Periscope from the command line, run the following Gradle command:

````
./gradlew :autoscale:bootRun -PjvmArgs="-Dperiscope.client.id=periscope \
-Dperiscope.client.secret=<UAA_DEFAULT_SECRET> \
-Dperiscope.identity.server.url=http://localhost:8089 \
-Dperiscope.db.port.5432.tcp.addr=localhost \
-Dperiscope.db.port.5432.tcp.port=5432 \
-Dperiscope.cloudbreak.url=http://localhost:8080 \
-Dperiscope.schema.scripts.location=$(pwd)/autoscale/src/main/resources/schema
-Dserver.port=8085 \
-Daltus.ums.host=localhost \
-Dvault.addr=localhost \
-Dvault.root.token=<VAULT_ROOT_TOKEN> \
-Dspring.config.location=$(pwd)/autoscale/src/main/resources/application.yml,$(pwd)/autoscale/build/resources/main/application.properties"
````

Replace `<UAA_DEFAULT_SECRET>` with the value of UAA_DEFAULT_SECRET from the cdb-local/Profile file. Replace `<VAULT_ROOT_TOKEN>` with the value of VAULT_ROOT_TOKEN from the cbd-local/Profile file.

### Datalake
To run Datalake from the command line, run the following Gradle command:

````
./gradlew :datalake:bootRun -PjvmArgs="-Ddatalake.db.env.address=localhost \
-Dserver.port=8086 \
-Ddatalake.cloudbreak.url=http://localhost:8080 \
-Dvault.addr=localhost \
-Dvault.root.token=<VAULT_ROOT_TOKEN> \
-Dspring.config.location=$(pwd)/datalake/src/main/resources/application.yml,$(pwd)/datalake/build/resources/main/application.properties"
````

### FreeIPA
To run the FreeIPA management service from the command line, run the following Gradle command:

````
./gradlew :freeipa:bootRun -PjvmArgs="-Dfreeipa.db.addr=localhost \
-Dserver.port=8090 \
-Dvault.addr=localhost \
-Dvault.root.token=<VAULT_ROOT_TOKEN> \
-Dspring.config.location=$(pwd)/freeipa/src/main/resources/application.yml,$(pwd)/freeipa/build/resources/main/application.properties"
````

Replace `<VAULT_ROOT_TOKEN>` with the value of VAULT_ROOT_TOKEN from the cbd-local/Profile file.

### Redbeams
To run Redbeams from the command line, run the following Gradle command:

```
./gradlew :redbeams:bootRun -PjvmArgs="-Dredbeams.client.id=redbeams \
-Dredbeams.client.secret=<UAA_DEFAULT_SECRET> \
-Dredbeams.identity.server.url=http://localhost:8089 \
-Dredbeams.db.port.5432.tcp.addr=localhost \
-Dredbeams.db.port.5432.tcp.port=5432 \
-Dredbeams.cloudbreak.url=http://localhost:8080 \
-Dredbeams.schema.scripts.location=$(pwd)/redbeams/src/main/resources/schema \
-Dserver.port=8087 \
-Daltus.ums.host=localhost \
-Dvault.addr=localhost \
-Dvault.root.token=<VAULT_ROOT_TOKEN> \
-Dspring.config.location=$(pwd)/redbeams/src/main/resources/application.yml,$(pwd)/redbeams/build/resources/main/application.properties"
```

Replace `<UAA_DEFAULT_SECRET>` with the value of UAA_DEFAULT_SECRET from the cdb-local/Profile file. Replace `<VAULT_ROOT_TOKEN>` with the value of VAULT_ROOT_TOKEN from the cbd-local/Profile file.

### Environment
To run the Environment service from the command line, run the following Gradle command:

```
./gradlew :environment:bootRun -PjvmArgs="\
-Dvault.root.token=<TOKEN_FROM_PROFILE_FILE> \
-Dspring.config.location=$(pwd)/environment/src/main/resources/application.yml,$(pwd)/environment/build/resources/main/application.properties"
```

Replace `<VAULT_ROOT_TOKEN>` with the value of VAULT_ROOT_TOKEN from the cbd-local/Profile file.

## Database development

If any schema change is required in Cloudbreak services databases (cbdb/environmentdb/datalakedb/redbeamsdb), then the developer needs to write SQL scripts to migrate the database accordingly. The schema migration is managed by [MyBatis Migrations](https://github.com/mybatis/migrations) in Cloudbreak and the cbd tool provides an easy-to-use wrapper for it. The syntax for using the migration commands is `cbd migrate <database name> <command> [parameters]` e.g. `cbd migrate cbdb status`.
Create a SQL template for schema changes:
```
cbd migrate cbdb new "CLOUD-123 schema change for new feature"
```
As as result of the above command an SQL file template is generated under the path specified in `CB_SCHEMA_SCRIPTS_LOCATION` environment variable, which is defined in Profile. The structure of the generated SQL template looks like the following:
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
If you would like to rollback the last SQL file, then just use the down command:
```
cbd migrate <database-name> down
```
On order to check the status of database
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

## How to use with DPS

Clone DPS repository from hortonworks/dps-platform

Put dps-platform path into your Profile:
```
export DPS_REPO=/Users/YOUR_USERNAME/DPS_PROJECT_DIR
```
By default it will build docker images from your dps-platform directory, but if you want to use a
specific version of DPS you can put the DPS_VERSION env variable into your Profile file.
In this case it will download images from the repository instead of building them.
```
export DPS_VERSION=2.0.0.0-132
```
If you want to force build the project with `docker build` use the FORCE_BUILD env variable.
```
export FORCE_BUILD=true
```

## Additional info

> * [Retrieve OAuth Bearer Token via Cloudbreak REST API](https://github.com/hortonworks/cloudbreak/blob/master/docs/common/retrieve_oauth_token.md)
> * [Cloudbreak Service Provider Interface (SPI)](https://github.com/hortonworks/cloudbreak/blob/master/docs/spi.md)
