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

#### Test cases
Test cases are located under folder `https://github.com/hortonworks/cloudbreak/tree/master/integration-test/src/main/java/com/sequenceiq/it/cloudbreak/testcase`:
 - Integration Tests: `mock`
 - End To End Tests: `e2e`
 - Smoke Tests: `smoke`
*Note:*
[E2E tests](https://github.com/hortonworks/cloudbreak/tree/master/integration-test/src/main/java/com/sequenceiq/it/cloudbreak/testcase/e2e) are organized into domain-specific packages (distrox, sdx etc.).

#### Cloud agnostic tests
Tests should be written in a cloud-vendor agnostic manner. Cloud-specific code should be hidden behind following interfaces:
- CloudFunctionality: code directly manipulating cloud platforms (e.g. start / stop instances)
- CloudProvider: DTOs containing cloud provider specific data 

Tests cases should thus reference cloud-vendor specific settings or make direct calls via these interfaces. If they are implemented for a given cloud vendor then the test should be able to run for that vendor.

If there are tests that contain DTOs with very specific cloud vendor dependent settings that cannot be generalized into an interface (like adls gen 2 settings of azure) then it might make sense to name the test as AzureAdlsGen2 test, but place it into a common package with other related tests (e.g. package name: storage, and aws s3 tests could also be placed there).

#### Suites of tests in yaml files
The previous organization of tests into cloud-vendor specific packages (e.g. aws tests) made it easy to see how many and what kind of tests are there for a given platform. Now, this information is not present any more and will be solely kept in the testsuites yaml files under `integration-test/src/main/resources/testsuites/e2e`.
To help the reader to find out what tests are available for a certain platform the following guidelines are suggested:
- keep a yaml file listing all implemented test for a given cloud vendor (e.g. `aws-e2e-tests.yaml`), and
- yaml files should __list classes and included methods__, and avoid using package scope as much as possible. Class and method names will pop up in textual search and are __rename aware__ in IDEA: 
    - method / class / package rename, 
    - class move
    - accidental moving or renaming of classes will be guarded by TestNG (will throw a not found exception on moved classes)
    - however, testNG currently does not fail if an included method is not found, just ignores it - a quite dangeorous behavior in my mind 
