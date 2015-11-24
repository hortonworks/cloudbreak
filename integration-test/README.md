# Integration test for Cloudbreak

This is a standalone springboot application with which we can run cloudbreak specific testng testsuites parallel.

## Build:
1. Build clodubreak with ./gradlew clean build:
2. The built jar file can be found in the integration-test/build/libs

## Run:
1. from the integrationtest directory:
2. java -jar build/libs/cloudbreak-integration-test-{versioninfo}.jar [program arguments]

## Configuration parameters:
In SpringBoot applications you can define the configuration parameters different ways:
http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html

Most of the configutation parameters with their default values (if any) can be found in the src/main/resources/application.yml file.
You can define your own application.yml file where you can overwrite the default parameters in the working directory where the integration test is running from, or you can define the SPRING_CONFIG_LOCATION environment variable or spring.config.location program argument with the directory of your application.yml.

### Mandatory parameters

* url of uaa server
* uaa user
* uaa password
* url of cloudbreak
* integrationtest command and related params

You can define these parameters in your application.yml:

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
```

or as program arguments:

```
java -jar <path of cloudbreak.jar> --integrationtest.uaa.server=http://192.168.59.103:8089 --integrationtest.uaa.user=example@example.com --integrationtest.uaa.password=password --integrationtest.cloudbreak.server=http://192.168.59.103:8080
```

or as environment variables:

```
export INTEGRATIONTEST_UAA_SERVER=http://192.168.59.103:8089
export INTEGRATIONTEST_UAA_USER=example@example.com
export INTEGRATIONTEST_UAA_PASSWORD=password
export INTEGRATIONTEST_CLOUDBREAK_SERVER=http://192.168.59.103:8080
```

### integrationtest command and related params
The following parameters are program arguments, starts with `'--'` (but can be defined other ways, read the above mentioned documentaion):
#### integrationtest.command
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
      blueprintName: it-blueprint-restest
      blueprintFile: classpath:/blueprint/multi-node-hdfs-yarn.bp
    classes:
      - com.sequenceiq.it.cloudbreak.BlueprintCreationTest
      - com.sequenceiq.it.cloudbreak.BlueprintDeleteByNameTest

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
# GCP credential name must be specified
name: Gcp_full_smoketest
parameters: {
  cloudProvider: GCP
}

tests:
  - name: init
    classes:
      - com.sequenceiq.it.TestSuiteInitializer
      - com.sequenceiq.it.cloudbreak.CloudbreakTestSuiteInitializer

  - name: create gateway template
    parameters: {
      gcpName: it-gcp-smoke-gateway-ssud,
      gcpInstanceType: n1-standard-4,
      volumeType: pd-standard,
      volumeCount: 1,
      volumeSize: 30,
      templateAdditions: "cbgateway,1,GATEWAY"
    }
    classes:
      - com.sequenceiq.it.cloudbreak.GcpTemplateCreationTest

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

  - name: create slave template
    parameters: {
      gcpName: it-gcp-smoke-slave-ssud,
      gcpInstanceType: n1-highcpu-4,
      volumeType: pd-ssd,
      volumeCount: 3,
      volumeSize: 500,
      templateAdditions: "slave_1,3"
    }
    classes:
      - com.sequenceiq.it.cloudbreak.GcpTemplateCreationTest

  - name: create cluster
    parameters: {
      stackName: it-gcp-stack-ssud,
      region: EUROPE_WEST1_B,
      clusterName: it-gcp-cluster-ssud
    }
    classes:
      - com.sequenceiq.it.cloudbreak.StackCreationTest
      - com.sequenceiq.it.cloudbreak.ClusterCreationTest

  - name: stop cluster
    parameters: {
      newStatus: STOPPED
    }
    classes:
      - com.sequenceiq.it.cloudbreak.StatusUpdateTest

  - name: start cluster
    parameters: {
      newStatus: STARTED
    }
    classes:
      - com.sequenceiq.it.cloudbreak.StatusUpdateTest

  - name: upscale
    parameters: {
      instanceGroup: slave_1,
      scalingAdjustment: 3
    }
    classes:
      - com.sequenceiq.it.cloudbreak.ScalingTest

  - name: downscale
    parameters: {
      instanceGroup: slave_1,
      scalingAdjustment: -2
    }
    classes:
      - com.sequenceiq.it.cloudbreak.ScalingTest
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
* blueprintName - the name of the blueprint
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
* Store named resources in the context if given on applcication or suite level: blueprint, network, securitygroup, stack, credential, instancegroups, hostgroups
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
* ClusterCreationTest
* CountRecipeResultsTest
* CredentialDeleteByNameTest
* NetworkDeleteByNameTest
* RecipeCreationTest
* ScalingTest
* StackCreationTest
* StatusUpdateTest

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

### Test results
* TestNG generated result can be found in the `test-output` directory under the working directory
* ReportNG generated result can be found in the `test-output/html` directory under the working directory
