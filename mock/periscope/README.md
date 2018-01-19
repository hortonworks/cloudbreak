# Periscope API mock

## Get SWAGGER Codegen

Install [Swagger Codegen CLI](https://github.com/swagger-api/swagger-codegen#prerequisites). 

CLI is also available as [Docker Image](https://hub.docker.com/r/swaggerapi/swagger-codegen-cli/). You can read further [here](https://github.com/swagger-api/swagger-codegen#swagger-codegen-cli-docker-image).

### Get Help
To get a list of available general options, you can run `help generate`. Here is an example for OS X users:
```java -jar /usr/local/Cellar/swagger-codegen/2.2.3/libexec/swagger-codegen-cli.jar help generate```

### NodeJS specific options
Supported config options can be different per language. Running `config-help -l {lang}` will show available options. Here is an example for OS X users to NodeJS:
```java -jar /usr/local/Cellar/swagger-codegen/2.2.3/libexec/swagger-codegen-cli.jar config-help -l nodejs-server```

### Generate NodeJS Client from QA
Here is an example for OS X users to generate only NodeJS APIs (without tests):
```
java -jar /usr/local/Cellar/swagger-codegen/2.2.3/libexec/swagger-codegen-cli.jar generate \
   -i https://qa-cloudbreak.eng.hortonworks.com/periscope/swagger.json \
   -l nodejs-server \
   -o javascript_api_client \
   -Dio.swagger.parser.util.RemoteUrl.trustAll=true \
   -Dapis -DapiTests=false
```

## Server stub generator HOWTO
[The documentation](https://github.com/swagger-api/swagger-codegen/wiki/Server-stub-generator-HOWTO) to generate a server stub for a couple different frameworks.

## Download Periscope SWAGGER JSON
```curl --insecure https://qa-cloudbreak.eng.hortonworks.com/periscope/swagger.json --output swagger.json```

## Generate Periscope API NodeJS Client
```
java -jar /usr/local/Cellar/swagger-codegen/2.2.3/libexec/swagger-codegen-cli.jar generate \
   -i swagger.json \
   -l nodejs-server \
   -o javascript_api_client
```

* `/usr/local/Cellar/swagger-codegen/2.2.3/libexec/swagger-codegen-cli.jar`: the Swagger Codegen CLI path on OS X. This can be vary on different OSs and with different installs. You can read more about this at [Swagger Codegen CLI Installation](https://github.com/swagger-api/swagger-codegen#table-of-contents).
* `javascript_api_client`: the destination folder for the generated client. This is also can be vary based on your decision. 

## Upgrade Swagger YML

### Base Path
Change `basePath: "/api"` to `basePath: "/as/api"` at [/api/swagger.yaml](api/swagger.yaml).

## Provide responses to all the needed Services
For existing services here is an example for `V2clustersService`:
```
exports.getByCloudbreakCluster = function(args, res, next) {
  /**
   * retrieve cluster
   * Ambari cluster.
   *
   * cbClusterId Long 
   * returns AutoscaleClusterResponse
   **/
  var examples = {};
  examples['application/json'] = {
  "port" : "aeiou",
  "stackId" : args.cbClusterId.value,
  "host" : "aeiou",
  "metricAlerts" : [ {
    "scalingPolicy" : {
      "adjustmentType" : "NODE_COUNT",
      "name" : "aeiou",
      "scalingAdjustment" : 2,
      "alertId" : 7,
      "hostGroup" : "aeiou"
    },
    "scalingPolicyId" : 5,
    "alertDefinition" : "aeiou",
    "period" : 5,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "id" : 1,
    "alertState" : "OK"
  } ],
  "id" : 6,
  "state" : "aeiou",
  "prometheusAlerts" : [ {
    "scalingPolicy" : "",
    "scalingPolicyId" : 1,
    "alertRuleName" : "aeiou",
    "period" : 4,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "threshold" : 7.386281948385884,
    "id" : 2,
    "alertState" : "OK",
    "alertOperator" : "LESS_THAN"
  } ],
  "autoscalingEnabled" : false,
  "user" : "aeiou",
  "timeAlerts" : [ {
    "cron" : "aeiou",
    "scalingPolicy" : "",
    "scalingPolicyId" : 3,
    "alertName" : "aeiou",
    "description" : "aeiou",
    "timeZone" : "aeiou",
    "id" : 9
  } ],
  "scalingConfiguration" : {
    "cooldown" : 6,
    "minSize" : 1,
    "maxSize" : 1
  }
};
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    switch(args.cbClusterId.value){
      case 1:
        var responseJson = {
          "host":null,
          "port":"9443",
          "user":"admin",
          "stackId":1,
          "id":1,
          "state":"CREATE_FAILED",
          "autoscalingEnabled":false,
          "metricAlerts":null,
          "timeAlerts":null,
          "prometheusAlerts":null,
          "scalingConfiguration":
          {  
            "minSize":3,
            "maxSize":100,
            "cooldown":30
          }
        };
        res.end(JSON.stringify(responseJson));
        break;
      case 2:
        var responseJson = {
          "host":"52.16.174.228",
          "port":"9443",
          "user":"admin",
          "stackId":2,
          "id":2,
          "state":"UPDATE_IN_PROGRESS",
          "autoscalingEnabled":false,
          "metricAlerts":null,
          "timeAlerts":null,
          "prometheusAlerts":null,
          "scalingConfiguration":
          {  
            "minSize":3,
            "maxSize":100,
            "cooldown":30
          }
        };
        res.end(JSON.stringify(responseJson));
        break;
      case 3:
        var responseJson = {
          "host":null,
          "port":"9443",
          "user":"admin",
          "stackId":3,
          "id":3,
          "state":"CREATE_IN_PROGRESS",
          "autoscalingEnabled":false,
          "metricAlerts":null,
          "timeAlerts":null,
          "prometheusAlerts":null,
          "scalingConfiguration":
          {  
            "minSize":3,
            "maxSize":100,
            "cooldown":30
          }
        };
        res.end(JSON.stringify(responseJson));
        break;
      default:
        res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
    }
  } else {
    res.end();
  }
}
```

## Periscope API Mock Docker Image
You can find at [Docker Hub](https://hub.docker.com/r/hortonworks/periscope-mock/)

### Build local image
You can build your own Periscope Mock image locally with:
```
docker build -t hortonworks/periscope-mock .
```

## Update CBD Profile with Periscope API Mock Image
First, initialize Periscope Mock by updating the CBD Profile file with the following content:
```
export DOCKER_IMAGE_CLOUDBREAK_PERISCOPE=hortonworks/periscope-mock
export DOCKER_TAG_PERISCOPE=latest
```

## Start Periscope Mock
To start the Periscope Mock application use the following command:
```
cbd start
```
This will start all the needed Docker containers and initialize the mock application.
