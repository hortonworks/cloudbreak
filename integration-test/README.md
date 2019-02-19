# Integration test for Cloudbreak

This is a standalone springboot application with which we can run cloudbreak specific testng testsuites parallel. 
This Springboot project wraps a TestNg framework, which actually runs the testcases.

#### Build:
1. Build cloudbreak project with ./gradlew clean build:
2. This will also produce the integration-test.jar . The built jar file can be found in the integration-test/build/libs

#### Cloudbreak configuration

1. For mock integration test to function you need to edit the spring configuration file located in cloud-common/src/resource/application.yml and add MOCK as a platform

    `cb.enabledplatforms: AZURE,AWS,GCP,OPENSTACK,MOCK`
2. Restart cloudbreak

## Configuration parameters:
Most of the configutation parameters with their default values (if any) can be found in the src/main/resources/application.yml file.
You can define your own application.yml file where you can overwrite the default parameters in the working directory where the integration test is running from, or 
you can define the SPRING_CONFIG_LOCATION environment variable or *spring.config.additional-location* program argument with the location of your application.yml.

### Quickstart setup of Integration tests with required parameters

* url of uaa server
* uaa user
* uaa password
* url of cloudbreak
* integrationtest command and related params

Change directory to integration-test and create your own application.yml with the following content.

* `docker-machine ip cbd` Should tell you the IP of uaa and cloudbreak server
* user/password can be found in your Profile inside cbd, (UAA_DEFAULT_USER_EMAIL, UAA_DEFAULT_USER_PASSWORD).

```
integrationtest:
    # uaa properties
    uaa:
        server: http://192.168.59.103:8089
        user: example@example.com
        password: password

    # cloudbreak properites
    cloudbreak:
        server: http://192.168.59.103:8080
        
    #working mode    
    command: suiteurls        
    suiteFiles:
      - classpath:/testsuites/v2/mock/v2-mock-stackcreate-scaling.yaml
    cleanup:
        cleanupBeforeStart: true        
```

These parameters can also be specified as program arguments or environment variables.
https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html

```
java -jar ./build/libs/cloudbreak-integration-test.jar -Dspring.config.additional-location="application.yml"
```
### Test results
* Test result can will be logged to STDOUT.
* TestNG generated result can be found in the `test-output` directory under the working directory
* ReportNG generated result can be found in the `test-output/html` directory under the working directory



#### integrationtest.command
Integration test framework supports multiple configuration modes. This can be defined with the *integrationtest.command* parameter.

##### smoketest
In this case you have to define the smoketests in the `'--integrationtest.testTypes'` program arguments as comma separated list.
The smoketests are collections of predefined testsuites, which you can find in the src/main/resources/testsuites.
You can find the smoketest definitons in src/main/resources/application.yml in the integrationtest.testSuites block.
For example:
```
java -jar <path of cloudbreak.jar> --integrationtest.command=smoketest --integrationtest.testTypes=GCP_FULL_SMOKE_TEST,AWS_FULL_SMOKE_TEST
```
##### fulltest
In this case couple of tests in differents regions will be run on all cloudprovider. The full tests are outdated, the usage of fulltest is not recommended.
In this case you have to define `'--integrationtest.fulltest.regindex'` and `'--integrationtest.fulltest.regnum'` parameters.
##### suites
In this case you have to define the suite files in the `'--integrationtest.suiteFiles'` program arguments as comma separated list.
*Supported file formats:* xml, yaml
For example:
```
java -jar <path of cloudbreak.jar> --integrationtest.command=suites --integrationtest.suiteFiles=/tmp/suite1.xml,/tmp/suite2.yaml
```
##### suiteurls
In this case you have to define the suite urls in the `'--integrationtest.suiteFiles'` program arguments as comma separated list.
*Url examples (nonexist):*
* file:///tmp/suite.xml
* classpath:/testsuites/azurerm/smoke/azurerm-clustercreate.yaml
* http://www.example.com/suite.xml (redirect is not handled)
For example:
```
java -jar <path of cloudbreak.jar> --integrationtest.command=suiteurls --integrationtest.suiteFiles=classpath:/testsuites/azurerm/smoke/azurerm-clustercreate.yaml,classpath:/testsuites/openstack/smoke/openstack-clustercreate.yaml
```

## Suites

TestNG support test suite definition in xml or yaml format.
The integration test contains predefined test suites in yaml format for all the supported cloud provider.
These suite files can be found under src/main/resources/testsuites directory.

under the cloudplatform specific directories (aws, azurerm, gcp, openstack, nativeos) there is a smoke directory. In this directory you can find the predefined smoke tests.
*Naming convention of smoketests:*
* {cloudprovider}-clustercreate.yml - test for cluster creation
* {cloudprovider}-clustercreate-startstop.yml - tests for cluster creation, stop and start
* {cloudprovider}-clustercreate-startstop-updown.yml - tests for cluster creation, stop, start, upscale and downscale
* {cloudprovider}-clustercreate-updown.yml - tests for cluster creation, upscale and downscale

In gcp there is a kerberos test under kerberos directory and a recipe test under recipe directory.

### Test suite example with credential / blueprint / template creation and deletion:

```
name: Resource_tests
parameters:
  cleanUp: false

tests:
  - name: init
    classes:
      - com.sequenceiq.it.TestSuiteInitializer
      - com.sequenceiq.it.cloudbreak.CloudbreakTestSuiteInitializer

  - name: gcp_template_test
    parameters:
      gcpName: it-gcp-template-restest
      templateName: it-gcp-template-restest
      gcpInstanceType: n1-standard-4
      volumeType: pd-standard
      volumeCount: 1
      volumeSize: 30
      templateAdditions: "gcp,1"
    classes:
      - com.sequenceiq.it.cloudbreak.GcpTemplateCreationTest
      - com.sequenceiq.it.cloudbreak.TemplateDeleteByNameTest

  - name: blueprint_test
    parameters:
      clusterDefinitionName: it-blueprint-restest
      clusterDefinitionFile: classpath:/blueprint/multi-node-hdfs-yarn.bp
    classes:
      - com.sequenceiq.it.cloudbreak.ClusterDefinitionCreationTest
      - com.sequenceiq.it.cloudbreak.ClusterDefinitionDeleteByNameTest

  # integratontest.gcpcredential params must be set in application.yml
  - name: gcp_credential_test
    parameters:
      credentialName: it-gcp-credential-restest
    classes:
      - com.sequenceiq.it.cloudbreak.GcpCredentialCreationTest
      - com.sequenceiq.it.cloudbreak.CredentialDeleteByNameTest
```

All test has its own parameter set which you have to define. There are application level, suite level and test level parameters.
In the example `cleanUp` is a suite level parameter and all the tests will get where it is defined as test parameter, but `gcpName` parameter only visible for the gcp_template_test tests.

### Test suite example with existing credential, blueprint resources:
```
name: OpenStack_full_smoketest_cred
parameters:
  cloudProvider: OPENSTACK
  clusterDefinitionName: testbp
  credentialName: openstack
  securityGroupName: all-services-port
  networkName: testosnetwork
  instanceGroups: osxlarge,cbgateway,1,GATEWAY;osxlarge,master,1,CORE;osxlarge,slave_1,3,CORE
  hostGroups: master,master,1;slave_1,slave_1,3

tests:
  - name: init
    classes:
      - com.sequenceiq.it.TestSuiteInitializer
      - com.sequenceiq.it.cloudbreak.CloudbreakTestSuiteInitializer

  - name: create cluster
    parameters:
      stackName: it-openstack-cred-ssud
      region: local
      clusterName: it-openstack-cred-ssud
    classes:
      - com.sequenceiq.it.cloudbreak.StackCreationTest
      - com.sequenceiq.it.cloudbreak.ClusterCreationTest

  - name: stop cluster and stack
    parameters:
      waitOn: true
    classes:
      - com.sequenceiq.it.cloudbreak.startstop.ClusterAndStackStopTest

  - name: start stack and cluster
    parameters:
      waitOn: true
    classes:
      - com.sequenceiq.it.cloudbreak.startstop.StackAndClusterStartTest

  - name: upscale stack, upscale cluster
    parameters:
     instanceGroup: slave_1
     scalingAdjustment: 4
    classes:
      - com.sequenceiq.it.cloudbreak.scaling.StackScalingTest
      - com.sequenceiq.it.cloudbreak.scaling.ClusterScalingTest

  - name: downscale cluster, downscale stack
    parameters:
     instanceGroup: slave_1
     scalingAdjustment: -2
    classes:
      - com.sequenceiq.it.cloudbreak.scaling.ClusterScalingTest
      - com.sequenceiq.it.cloudbreak.scaling.StackScalingTest

```

In the example `cloudProvider` is a suite level parameter and `CloudbreakTestSuiteInitializer` will initialize the test context based on the application level `gcp` parameters.

#### Suite level parameter

**Basic parameters (overwrite the environment variable for the actual suite):**
* uaaServer - url of uaa server
* uaaUser - uaa user
* uaaPassword - uaa password
* cloudbreakServer - url of the cloud break server

**Resource names if we have existing ones:**
* credentialName - the name of the credential
* clusterDefinitionName - the name of the blueprint
* stackName - the name of the cluster
* instanceGroups - instance groups for cluster creation,

format:

    <template name 1>,<hostgroup 1>,<nodecount 1>;...;<template name n>,<hostgroup n>,<nodecount n>

example:

    it-gcp-master,master,1;it-gcp-slave,slave_1,3

## Architecture

### Main class

com.sequenceiq.it.IntegrationTestApp

### Initialization, context building

*com.sequenceiq.it.TestSuiteInitializer*
* Responsible for the creation of independent suite level contexts for all the suites.
* Creates and stores authentication token from the oath server

*com.sequenceiq.it.cloudbreak.CloudbreakTestSuiteInitializer*
* Initialize cloudbreak test context for the suites
* Store named resources in the context if given on applcication or suite level: blueprint, network, securitygroup, stack, credential, instancegroups
* Cleanup after the testsuite finished

All the suite have to be started with these classes:

```
tests:
  - name: init
    classes:
      - com.sequenceiq.it.TestSuiteInitializer
      - com.sequenceiq.it.cloudbreak.CloudbreakTestSuiteInitializer
```

### Tests

**Common, not cloudprovider specific tests:**
* BlueprintCreationTest
* BlueprintDeleteByNameTest
* ClusterAndStackDownscaleTest
* ClusterAndStackStopTest
* ClusterCreationTest
* ClusterScalingTest
* ClusterStartTest
* ClusterStopTest
* CountRecipeResultsTest
* CredentialDeleteByNameTest
* NetworkDeleteByNameTest
* RecipeCreationTest
* RDSConfigTest
* StackAndClusterUpscaleTest
* StackAndClusterStartTest
* StackCreationTest
* StackScalingTest
* StackStartTest
* StackStopTest


**Cloud provider specific tests:**
* Credential creation tests
  * AwsCredentialCreationTest
  * GcpCredentialCreationTest
  * AzureRmCredentialCreationTest
  * OpenStackCredentialCreationTest
* Template creation tests
  * AwsTemplateCreationTest
  * GcpTemplateCreationTest
  * AzureTemplateCreationTest
  * OpenStackTemplateCreationTest
* Network creation tests
  * OpenStackNetworkCreationTest

In a suite you can assemble the above mentioned tests in given order. Every suite has its own context in which the tests can share information with each other. (for example BlueprintCreationTest put the created blueprint's id into the context which is necessary for stack creation, etc)
You can define parameters for the tests as well, for example GcpTempateCreationTest:

```
  - name: create master template
    parameters: {
      gcpName: it-gcp-smoke-master-ssud,
      gcpInstanceType: n1-highmem-8,
      volumeType: pd-standard,
      volumeCount: 2,
      volumeSize: 100,
      templateAdditions: "master,1"
    }
    classes:
      - com.sequenceiq.it.cloudbreak.GcpTemplateCreationTest
```