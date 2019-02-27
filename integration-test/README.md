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

