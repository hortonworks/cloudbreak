# Integration test for Cloudbreak

This is a standalone springboot application with which we can run cloudbreak specific testng testsuites parallel. 
This Springboot project wraps a TestNg framework, which actually runs the testcases.

#### Build:
1. Build cloudbreak project with ./gradlew build:
2. This will also produce the integration-test.jar. The built jar file can be found in the integration-test/build/libs

#### Quickstart setup of Integration tests with required parameters

1. You need a config file under '~/.dp/config' with (this is easily can be achieved by installing a DP CLI and then logging in):

```
localhost:
  server: 127.0.0.1
  refreshtoken: {token}
```

#### How to run the integration tests from Intellij IDEA

1. Create a TestNG Run configuration
2. In the 'Suite' input field provide a file path like /Users/{username}/prj/cloudbreak/integration-test/src/main/resources/test-suite.xml (example file in integration-test/src/main/resources/test-suite.xml)

### Code organization

#### Test cases
Test cases are located under folder `/integration-test/src/main/java/com/sequenceiq/it/cloudbreak/testcase/e2e` and are organized into domain-specific packages (distrox, sdx etc.). The previous structuring by cloud vendor is to go away as much as possible. 

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
