# Integration test for Cloudbreak

This is a standalone [Spring Boot](https://spring.io/projects/spring-boot) application. We can run [Cloudbreak](https://docs.cloudera.com/HDPDocuments/Cloudbreak/Cloudbreak-2.9.0/introduction/content/cb_what-is-cloudbreak.html) specific [TestNG](https://testng.org/doc/) test suites in parallel. 

## Build
1. Build the [Integration Test](https://github.com/hortonworks/cloudbreak/tree/master/integration-test) project with:
 ```./gradlew :integration-test:clean build -x test```
 *Note:*
 ```-x test``` is optional here
2. After a successful build, you should find the `integration-test.jar` file at `integration-test/build/libs/`

## Quickstart setup of Integration tests with required parameters
1. You need a config file under `~/.dp/config`
 *Note:*
 This can be achieved by installing the [DP CLI](https://github.com/hortonworks/cb-cli) on your machine then [configure](https://github.com/hortonworks/cb-cli#configure) it.
 ```dp configure --server localhost --workspace your@email --profile localhost```
2. For API testing you should have a `localhost` profile in your DP Config file:
 ```
 localhost:
   server: localhost
   apikeyid: ***
   privatekey: ***
 ```
 *Note:*
 You should generate your own API Key and Private Key for local your development environment:
 ```
 dp generate-mock-apikeys --tenant-name default --tenant-user your@email
 ```
## Run Integration Tests in IDEA
To launch the Integration Test application you should execute the `com.sequenceiq.it.IntegrationTestApp` class (set `Use classpath of module` to `cloudbreak.integration-test.main`) to pointing your own `application.yml` as a JVM option.

### IntegrationTestApp Run Configuration﻿
1. Create your own and separate test folder on your local machine, for example `~/integration-test`
2. Create your own `aplication.yml` file there
    - You should create a local copy of [application.yml](https://github.com/hortonworks/cloudbreak/blob/master/integration-test/src/main/resources/application.yml)
    - You should update your application YAML file with all your secrets that are needed for your test(s)
    - `cloudProvider` should be updated correctly based on your tested provider (AWS, AZURE, YARN)
    - Server URLs/addresses should be updated correctly based on your tested environment (for integration testing: http://localhost)
3. Open the *Run/Debug Configuration* dialog
    - Click add (+) on the toolbar
    - The list shows the default run/debug configurations. Select the *Application* configuration type

#### Set up the IntegrationTestApp run/debug configuration:
1. Name: It is up to you
2. Main Class: 
 `com.sequenceiq.it.IntegrationTestApp`
3. VM Options:
 `-Dspring.config.additional-location=/Users/[YOUR USER FOLDER]/integration-test/application.yml`
4. Program arguments:
     ```
     --integrationtest.command=suiteurls
     --mock.server.address=127.0.0.1
     --integrationtest.testsuite.threadPoolSize=3
     --integrationtest.suiteFiles=classpath:/testsuites/v4/mock/all-in-mock-package.yaml
     ```
5. Environment variables:
     ```
     INTEGRATIONTEST_CLOUDPROVIDER=MOCK
     INTEGRATIONTEST_USER_CRN=crn:altus:iam:us-west-1:cloudera:user:[YOUR USER]@cloudera.com
     INTEGRATIONTEST_TESTSUITE_POLLINGINTERVAL=1000
     ```
6. Use classpath of module: 
 `cloudbreak.integration-test.main`

## Run E2E Tests on your local machine
You should have a running [Cloudbreak](https://github.com/hortonworks/cloudbreak) before start testing, and after a successful build, you should have the `integration-test.jar` file at `integration-test/build/libs/`.

### Run a selected E2E test in Terminal
1. Create your own and separate test folder on your local machine, for example `~/integration-test`
2. Create your own `aplication.yml` file there
    - You should create a local copy of [application.yml](https://github.com/hortonworks/cloudbreak/blob/master/integration-test/src/main/resources/application.yml)
    - You should update your application YAML file with all your secrets that are needed for your test(s)
    - `cloudProvider` should be updated correctly based on your tested provider (AWS, AZURE, YARN)
    - Server URLs/addresses should be updated correctly based on your tested environment (for integration testing: http://localhost)
3. Copy the previously generated JAR (as a result of the Gradle build) to your own test folder:
 `cp -f integration-test/build/libs/*.jar ~/integration-test/`
4. Run a selected E2E test:
 `java -jar cloudbreak-integration-test.jar --integrationtest.command=suiteurls --integrationtest.suiteFiles=file:/Users/[YOUR USER FOLDER]/[YOUR CLOUDBREAK FOLDER]/integration-test/src/main/resources/testsuites/e2e/aws-sdx-upgrade-tests.yaml`
 *Note:*
 You should have a previously created YAML [test suite](https://github.com/hortonworks/cloudbreak/tree/master/integration-test/src/main/resources/testsuites) file for your test, in this case `aws-sdx-upgrade-tests.yaml`

## Code organization

### Test cases
Test cases are located under folder `https://github.com/hortonworks/cloudbreak/tree/master/integration-test/src/main/java/com/sequenceiq/it/cloudbreak/testcase`:
- Integration Tests: `mock`
- End To End Tests: `e2e`
- Smoke Tests: `smoke`
- Real UMS Tests: `authorization` ([UMS host and cache timeout values](#locally-running-authorization-tests) needs to be changed before running these tests)

*Note:*
[E2E tests](https://github.com/hortonworks/cloudbreak/tree/master/integration-test/src/main/java/com/sequenceiq/it/cloudbreak/testcase/e2e) are organized into domain-specific packages (distrox, sdx etc.).

### Cloud agnostic tests
Tests should be written in a cloud-vendor agnostic manner. Cloud-specific code should be hidden behind following interfaces:
- CloudFunctionality: code directly manipulating cloud platforms (e.g. start / stop instances)
- CloudProvider: DTOs containing cloud provider specific data 

Tests cases should thus reference cloud-vendor specific settings or make direct calls via these interfaces. If they are implemented for a given cloud vendor then the test should be able to run for that vendor.

If there are tests that contain DTOs with very specific cloud vendor dependent settings that cannot be generalized into an interface (like adls gen 2 settings of azure) then it might make sense to name the test as AzureAdlsGen2 test, but place it into a common package with other related tests (e.g. package name: storage, and aws s3 tests could also be placed there).

### Suites of tests in yaml files
The previous organization of tests into cloud-vendor specific packages (e.g. aws tests) made it easy to see how many and what kind of tests are there for a given platform. Now, this information is not present any more and will be solely kept in the testsuites yaml files under `integration-test/src/main/resources/testsuites/e2e`.
To help the reader to find out what tests are available for a certain platform the following guidelines are suggested:
- keep a yaml file listing all implemented test for a given cloud vendor (e.g. `aws-e2e-tests.yaml`), and
- yaml files should __list classes and included methods__, and avoid using package scope as much as possible. Class and method names will pop up in textual search and are __rename aware__ in IDEA: 
    - method / class / package rename, 
    - class move
    - accidental moving or renaming of classes will be guarded by TestNG (will throw a not found exception on moved classes)
    - however, testNG currently does not fail if an included method is not found, just ignores it - a quite dangeorous behavior in my mind 

## Authorization tests
Real UMS and Legacy AuthZ test cases are special kind of Mock tests:
- use real life Manowar Dev UMS
- use mocked cloud-vendor infrastructure (so any kind of AWS/Azure/GCP real resource will not be spin up)
- internally use CBD just like a regular integration test

Related test cases are present at [testcase/authorization](src/main/java/com/sequenceiq/it/cloudbreak/testcase/authorization) folder.

### Locally running Authorization tests
- `make fetch-secrets` is needed to initialize real UMS or Legacy users for authorization tests. *Fetching secrets accomplished via Azure CLI from Cloudbreak Team Azure Key Vault.*
    - [Install the Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest) on your local machine if it has not been installed yet. Read through the [Sign in with Azure CLI](https://docs.microsoft.com/en-us/cli/azure/authenticate-azure-cli?view=azure-cli-latest) documentation.
- follow [internal wiki](https://cloudera.atlassian.net/wiki/spaces/ENG/pages/626622557/Local+CB+with+Remote+UMS) about setting up remote UMS for your local setup

## L0 Promotion tests
Cloudbreak related [promotion tests](src/main/java/com/sequenceiq/it/cloudbreak/testcase/e2e/l0promotion) from [Management Console/Control Plane test repository](https://github.infra.cloudera.com/CDH/cdpmc-qe).

These tests are running on [Manowar-Dev](https://cloudera.dps.mow-dev.cloudera.com) in a separate test account (f8e2f110-fc7e-4e46-ae55-381aacc6718c) with machine users.

Selected [promotion test cases](https://github.infra.cloudera.com/CDH/cdpmc-qe/tree/master/tests/regression) for transition (from Management Console/Control Plane Regression tests):
- add user to environment
- modify environment user role
- add user to environment with multiple roles
- remove user from environment
- add user group to environment
- remove user group from environment
- add multiple groups to environments
- access Cloudera Manager with new workload password
- SSH access to Datalake

***Note:***
*In the future we can extend these test cases and the L0 package itself as well.*

### Locally running L0 Promotion tests
- `make fetch-secrets` is needed to initialize L0 users for tests. *Fetching secrets accomplished via Azure CLI from Cloudbreak Team Azure Key Vault.*
    - [Install the Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest) on your local machine if it has not been installed yet. Read through the [Sign in with Azure CLI](https://docs.microsoft.com/en-us/cli/azure/authenticate-azure-cli?view=azure-cli-latest) documentation.
    - Some L0 related UMS user store variable need to be set:
    ```
      export INTEGRATIONTEST_UMS_JSONSECRET_VERSION=9793d2c1afa84cbb9fdfeaee2f4327db
      export INTEGRATIONTEST_UMS_JSONSECRET_DESTINATIONPATH=./src/main/resources/ums-users/api-credentials.json
      export INTEGRATIONTEST_UMS_JSONSECRET_NAME=l0-ums-users-dev
    ```
    - Azure login applies [service principal authentication](https://docs.microsoft.com/en-us/cli/azure/create-an-azure-service-principal-azure-cli#sign-in-using-a-service-principal).
        - Service principal is the local application instance, of a global application object in a single tenant. The service principal object defines what the app can actually do in the specific tenant, who can access the app, and what resources the app can access.
        - Sign in with a service principal requires:
            - `AZURE_CLIENT_ID`: application ID that has access to the `jenkins-secret` key vault
            - `AZURE_CLIENT_SECRET`: application password
            - `AZURE_TENANT_ID`: tenant ID which is associated with the selected application
- Run the predefined test suites for L0 promotion. These suite files can be found at [src/main/resources/testsuites/e2e/l0promotion](src/main/resources/testsuites/e2e/l0promotion) directory. Tests can be [run in IDEA](#run-integration-tests-in-idea) or [in Terminal](#run-a-selected-e2e-test-in-terminal).
