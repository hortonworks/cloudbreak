# Cloudbreak

[![Maintainability](https://api.codeclimate.com/v1/badges/566493a63aaaf0c61bd4/maintainability)](https://codeclimate.com/github/hortonworks/cloudbreak/maintainability)
[![Build Automated](https://img.shields.io/docker/automated/hortonworks/cloudbreak.svg)](https://hub.docker.com/r/hortonworks/cloudbreak/)
[![Build Pulls](https://img.shields.io/docker/pulls/hortonworks/cloudbreak.svg)](https://hub.docker.com/r/hortonworks/cloudbreak/)
[![Licence](https://img.shields.io/github/license/hortonworks/cloudbreak.svg)](https://github.com/hortonworks/cloudbreak/blob/fix-readme/LICENSE)

* Documentation: https://docs.cloudera.com/management-console/cloud/index.html

# Local Development Setup
As of now this document focuses on setting up your development environment on macOS. You'll need Homebrew to install certain components in case you don't have them already. To get Homebrew please follow the install instructions on the Homebrew homepage: https://brew.sh

As a prerequisite, you need to have Java 11 installed. You can choose from many options, including the Oracle JDK, Oracle OpenJDK, or an OpenJDK from any of several providers. For help in choosing your JDK, consult [Java is Still Free](https://medium.com/@javachampions/java-is-still-free-2-0-0-6b9aa8d6d244).

You'll need Docker. For Mac, use [Docker Desktop for Mac](https://docs.docker.com/docker-for-mac/install/). Please allocate at least 5 CPU and 8GB Memory to the process.

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

Add the following to the file named `Profile` under the `cbd-local` directory you have just created. Please note, when a `cbd` command is executed you should go to the deployment's directory where your `Profile` file is found (`cbd-local` in our example). The `CB_SCHEMA_SCRIPTS_LOCATION` environment variable configures the location of SQL scripts that are in the `core/src/main/resources/schema` directory in the cloned Cloudbreak git repository.

Please note that the full path needs to be configured and env variables like `$USER` cannot be used. You also have to set a password for your local Cloudbreak in `UAA_DEFAULT_USER_PW`:

```
export CB_LOCAL_DEV_LIST=
export UAA_DEFAULT_SECRET=cbsecret2015
export CB_SCHEMA_SCRIPTS_LOCATION=/Users/YOUR_USERNAME/YOUR_PROJECT_DIR/cloudbreak/core/src/main/resources/schema
export ENVIRONMENT_SCHEMA_SCRIPTS_LOCATION=/Users/YOUR_USERNAME/YOUR_PROJECT_DIR/environment/src/main/resources/schema
export ULU_SUBSCRIBE_TO_NOTIFICATIONS=true
export CB_INSTANCE_UUID=$(uuidgen | tr '[:upper:]' '[:lower:]')
export CB_INSTANCE_NODE_ID=5743e6ed-3409-420b-b08b-f688f2fc5db1
export PUBLIC_IP=localhost
export VAULT_AUTO_UNSEAL=true
export DPS_VERSION=2.0.0.0-142
```

If you are using AWS, then also add the following lines, substituting your control plane AWS account id, and the AWS credentials that you have created for the CB role.

```
export CB_AWS_ACCOUNT_ID="YOUR_AWS_ACCOUNT_ID"
export AWS_ACCESS_KEY_ID="YOUR_ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="YOUR_SECRET_KEY"

```

At first, you should start every service from cbd to check that your cloud environment is set up correctly.
```
export CB_LOCAL_DEV_LIST=
```

When this setup works, you can remove services from cbd, and run them locally. For example, in order to run Cloudbreak, Periscope, Datalake, FreeIPA, Redbeams, Environment, Auth Mock (and CAAS API), IDBroker Mapping Management, and Environments2 API services locally (from IDEA or the command line), put this into your `Profile`:
```
export CB_LOCAL_DEV_LIST=cloudbreak,periscope,datalake,freeipa,redbeams,environment,auth-mock,caas-api,idbmms,environments2-api
```

Containers for these applications won't be started and Uluwatu (or the `cdp` & `dp` CLI tools) will connect to Java processes running on your host.
You don't have to put all of the applications into local-dev mode; the value of the variable could be any combination.

You need to login to DockerHub:
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

For some reason if you encounter a similar problem with Periscope, Datalake, FreeIPA, Redbeams, or Environment, then run the following commands and you can restart the Cloudbreak Deployer:
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
```
You can track any other application's logs to check the results by executing the following command:
```
cbd logs periscope # or datalake, freeipa, redbeams, environment, auth-mock, idbmms, environments2-api
```

If everything went well then Cloudbreak will be available on https://localhost. For more details and config parameters please check the documentation of [Cloudbreak Deployer](https://github.com/hortonworks/cloudbreak-deployer).

The deployer has generated a `certs` directory under `cbd-local` directory which will be needed later on to set up IDEA properly.

If not already present, you shall create an `etc` directory under `cbd-local` directory and place your Cloudera Manager license file `license.txt` there. This is essential for Auth Mock to start successfully. (Request a licence from us)

### Cloudbreak service ports

When cloudbreak is started in a container, the port it listens on is 8080.  If "cloudbreak" is added to the CB_LOCAL_DEV_LIST variable, all services expect the cloudbreak port to be 9091.

### Linux difference

Cloudbreak Deployer is unable to determine the IP address on a Linux machine. Therefore, you must add in the public IP address manually to your `Profile`.

```
export PUBLIC_IP=127.0.0.1
```

## IDEA

### Check out the Cloudbreak repository

Go to https://github.com/hortonworks/cloudbreak, either clone or download the repository, use SSH which is described here: https://help.github.com/articles/connecting-to-github-with-ssh/

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

Cloudbreak can be imported into IDEA as a Gradle project by specifying the `cloudbreak` repo root under Import Project. Once it is done, you need to import the proper code formatter by using the `File -> Import Settings...` menu and selecting the `idea_settings.jar` located in the `config/idea` directory in the Cloudbreak git repository.

Also you need to import inspection settings called `inpsections.xml` located in `config/idea`:
```
IntelliJ IDEA -> Preferences -> Editor -> Inspections -> Settings icon -> Import Profile
```

Cloudbreak integrates with GRPC components. This results in generated files inside the project with big file sizes. By default IDEA ignores anything that is more than 8MB, resulting in unknown classes inside the IDEA context. To circumvent this, you need to add this property to your IDEA properties.

Go to `Help -> Edit Custom Properties...`, then insert
```
#parse files up until 15MB
idea.max.intellisense.filesize=15000
```
Restart IDEA, and Rebuild.

### Running Cloudbreak in IDEA

To launch the Cloudbreak application execute the `com.sequenceiq.cloudbreak.CloudbreakApplication` class (set `Use classpath of module` to `cloudbreak.core.main`) with the following JVM options:
```
-Dcb.db.port.5432.tcp.addr=localhost
-Dcb.db.port.5432.tcp.port=5432
-Dserver.port=9091
-Daltus.ums.host=localhost
-Dvault.addr=localhost
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Dcb.enabledplatforms=""
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

Afterward add these entries to the environment variables (the same values the you set in Profile):
```
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
CB_AWS_ACCOUNT_ID=
```

The database migration scripts are run automatically by Cloudbreak, but this migration can be turned off with the `-Dcb.schema.migration.auto=false` JVM option.

### Configure Before launch task

In order to be able to determine the local Cloudbreak version automatically, a `Before launch` task has to be configured for the project in IntelliJ IDEA. The required steps are the following:

1. Open `Run/Debug Configurations` for the project
2. Select your project's application
3. Click on `Add` in the `Before launch` panel
4. Select `Run Gradle Task` with the following parameters
    1. `Gradle project`: `cloudbreak:core`
    2. `Tasks`: `buildInfo`
5. Confirm and restart the application

### Running Periscope in IDEA

After importing the `cloudbreak` repo root, launch the Periscope application by executing the `com.sequenceiq.periscope.PeriscopeApplication` class (set `Use classpath of module` to `cloudbreak.autoscale.main`) with the following JVM options:
* Note: If cloudbreak is in the CB_LOCAL_DEV_LIST variable, the periscope.cloudbreak.url should be http://localhost:9091
````
-Dperiscope.db.port.5432.tcp.addr=localhost
-Dperiscope.db.port.5432.tcp.port=5432
-Dperiscope.cloudbreak.url=http://localhost:8080
-Dserver.port=8085
-Daltus.ums.host=localhost
-Dvault.root.token=<VAULT_ROOT_TOKEN>
````

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.

### Running Datalake in IDEA

After importing the `cloudbreak` repo root, launch the Datalake application by executing the `com.sequenceiq.datalake.DatalakeApplication` class (set `Use classpath of module` to `cloudbreak.datalake.main`) with the following JVM options:
* Note: If cloudbreak is in the CB_LOCAL_DEV_LIST variable, the datalake.cloudbreak.url should be http://localhost:9091
````
-Dserver.port=8086
-Dcb.enabledplatforms=AWS,AZURE,MOCK
-Ddatalake.cloudbreak.url=http://localhost:8080
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Dvault.addr=localhost
````

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.

### Running FreeIPA in IDEA

After importing the `cloudbreak` repo root, launch the FreeIPA application by executing the `com.sequenceiq.freeipa.FreeIpaApplication` class (set `Use classpath of module` to `cloudbreak.freeipa.main`) with the following JVM options:

````
-Dfreeipa.db.addr=localhost
-Dserver.port=8090
-Dvault.root.token=<VAULT_ROOT_TOKEN>
````

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.

then add these entries to the environment variables (the same values the you set in Profile):
```
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
CB_AWS_ACCOUNT_ID=
```

### Running Redbeams in IDEA

After importing the `cloudbreak` repo root, launch the Redbeams application by executing the `com.sequenceiq.redbeams.RedbeamsApplication` class (set `Use classpath of module` to `cloudbreak.redbeams.main`) with the following JVM options:
* Note: If cloudbreak is in the CB_LOCAL_DEV_LIST variable, the redbeams.cloudbreak.url should be http://localhost:9091
```
-Dredbeams.db.port.5432.tcp.addr=localhost
-Dredbeams.db.port.5432.tcp.port=5432
-Dredbeams.cloudbreak.url=http://localhost:8080
-Dserver.port=8087
-Daltus.ums.host=localhost
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Dcb.enabledplatforms=AWS,AZURE,MOCK
```

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.

then add these entries to the environment variables (the same values the you set in Profile):
```
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
CB_AWS_ACCOUNT_ID=
```

### Running the Environment service in IDEA

After importing the `cloudbreak` repo root, launch the Environment application by executing the `com.sequenceiq.environment.EnvironmentApplication` class (set `Use classpath of module` to `cloudbreak.environment.main`) with the following JVM options:
* Note: If cloudbreak is in the CB_LOCAL_DEV_LIST variable, the environment.cloudbreak.url should be http://localhost:9091
```
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Denvironment.cloudbreak.url=http://localhost:8080
-Denvironment.enabledplatforms="YARN,YCLOUD,AWS,AZURE,MOCK"
```

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.

then add these entries to the environment variables (the same values the you set in Profile):
```
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
CB_AWS_ACCOUNT_ID=
```

### Running Auth Mock in IDEA

After importing the `cloudbreak` repo root, launch the Auth Mock application by executing the `com.sequenceiq.caas.MockCaasApplication` class (set `Use classpath of module` to `cloudbreak.mock-caas.main`) with the following JVM options:

```
-Dauth.config.dir=<CBD_LOCAL_ETC>
```

Replace `<CBD_LOCAL_ETC>` with the full path of your `cbd-local/etc` directory that shall already contain the Cloudera Manager license file `license.txt`.

Please make sure that `caas-api` has been added to `CB_LOCAL_DEV_LIST` list in `Profile` file of cbd.

## Command line

### Running Cloudbreak from the Command Line
To run Cloudbreak from the command line first set the the AWS environment variables (use the same values as in Profile)

```
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export CB_AWS_ACCOUNT_ID=...
```

then run the following Gradle command:

```
./gradlew :core:buildInfo :core:bootRun --no-daemon -PjvmArgs="-Dcb.db.port.5432.tcp.addr=localhost \
-Dcb.db.port.5432.tcp.port=5432 \
-Dcb.schema.scripts.location=$(pwd)/core/src/main/resources/schema
-Dserver.port=9091 \
-Daltus.ums.host=localhost
-Dvault.root.token=<VAULT_ROOT_TOKEN>
-Dspring.config.location=$(pwd)/core/src/main/resources/application.yml,$(pwd)/core/build/resources/main/application.properties"
```

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.


The database migration scripts are run automatically by Cloudbreak, this migration can be turned off with the `-Dcb.schema.migration.auto=false` JVM option.

### Running Periscope from the Command Line
To run Periscope from the command line, run the following Gradle command:
* Note: If cloudbreak is in the CB_LOCAL_DEV_LIST variable, the periscope.cloudbreak.url should be http://localhost:9091
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
* Note: If cloudbreak is in the CB_LOCAL_DEV_LIST variable, the datalake.cloudbreak.url should be http://localhost:9091
````
./gradlew :datalake:bootRun -PjvmArgs="-Dvault.root.token=<VAULT_ROOT_TOKEN> \
-Dserver.port=8086 \
-Ddatalake.cloudbreak.url=http://localhost:8080
-Dspring.config.location=$(pwd)/datalake/src/main/resources/application.yml,$(pwd)/datalake/build/resources/main/application.properties"
````

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.

### Running FreeIPA from the Command Line
To run the FreeIPA management service from the command line first set the the AWS environment variables (use the same values as in Profile)

```
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export CB_AWS_ACCOUNT_ID=...
```

then run the following Gradle command:

````
./gradlew :freeipa:bootRun --no-daemon -PjvmArgs="-Dfreeipa.db.addr=localhost \
-Dserver.port=8090 \
-Dvault.root.token=<VAULT_ROOT_TOKEN> \
-Dspring.config.location=$(pwd)/freeipa/src/main/resources/application.yml,$(pwd)/freeipa/build/resources/main/application.properties"
````

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.

### Running Redbeams from the Command Line
To run the Redbeams from the command line first set the the AWS environment variables (use the same values as in Profile)

```
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export CB_AWS_ACCOUNT_ID=...
```

then run the following Gradle command:
* Note: If cloudbreak is in the CB_LOCAL_DEV_LIST variable, the redbeams.cloudbreak.url should be http://localhost:9091
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

### Running the Environment service from the Command Line
To run the Environment service from the command line first set the the AWS environment variables (use the same values as in Profile)

```
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export CB_AWS_ACCOUNT_ID=...
```

then run the following Gradle command:
* Note: If cloudbreak is in the CB_LOCAL_DEV_LIST variable, the environment.cloudbreak.url should be http://localhost:9091
```
./gradlew :environment:bootRun -PjvmArgs="\
-Denvironment.cloudbreak.url=http://localhost:8080 \
-Dvault.root.token=<VAULT_ROOT_TOKEN> \
-Dspring.config.location=$(pwd)/environment/src/main/resources/application.yml,$(pwd)/environment/build/resources/main/application.properties"
```

Replace `<VAULT_ROOT_TOKEN>` with the value of `VAULT_ROOT_TOKEN` from the `Profile` file.

### Running Auth Mock from the Command Line
To run Auth Mock from the command line, run the following Gradle command:

```
./gradlew :mock-caas:bootRun -PjvmArgs="\
-Dserver.port=10080 \
-Dauth.config.dir=<CBD_LOCAL_ETC> \
-Dspring.config.location=$(pwd)/mock-caas/src/main/resources/application.yml"
```

Replace `<CBD_LOCAL_ETC>` with the full path of your `cbd-local/etc` directory that shall already contain the Cloudera Manager license file `license.txt`.

Please make sure that `caas-api` has been added to `CB_LOCAL_DEV_LIST` list in `Profile` file of cbd.

## Database development

If any schema change is required in Cloudbreak services databases (`cbdb` / `periscopedb` / `datalakedb` / `redbeamsdb` / `environmentdb` / `freeipadb`), then the developer needs to write SQL scripts
 to migrate the database accordingly. The schema migration is managed by [MyBatis Migrations](https://github.com/mybatis/migrations) in Cloudbreak and the cbd tool provides an easy-to-use wrapper for it. The syntax for using the migration commands is `cbd migrate <database name> <command> [parameters]` e.g. `cbd migrate cbdb status`.
Create a SQL template for schema changes:
```
cbd migrate cbdb new "CLOUD-123 schema change for new feature"
```
As as result of the above command an SQL file template is generated under the path specified in `CB_SCHEMA_SCRIPTS_LOCATION` environment variable, which is defined in `Profile`. The structure of the generated SQL template looks like the following:
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

## How to reach CM UI directly(not through Knox)
With the current design on the cluster's gateway node there is an NGiNX which is responsible for routing requests through Knox by default. 
But there are cases when the CM UI needs to be reached directly. It is possible on the same port by the same NGiNX on the `clouderamanager/` path of the provisioned cluster.
Please note that the trailing slash is significant for the routing to work.

For example: `https://tb-nt-local.tb-local.xcu2-8y8x.workload-dev.cloudera.com/clouderamanager/`

> Be aware of that this routing mechanism is based on cookies, so if you have problems to reach the CM UI directly especially when you reached any service through Knox previously then the deletion of cookies could solve your issues.

## Additional info
> * [Cloudbreak Service Provider Interface (SPI)](https://github.com/hortonworks/cloudbreak/blob/master/docs/spi.md)
