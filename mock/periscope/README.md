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
Change `basePath: "/api"` to `basePath: "/as"` at [/api/swagger.yaml](api/swagger.yaml).

### New Info and Health Services
Introduce brand new Services for Periscope Health.

1. In the `tags` section:
```
tags:
- name: "health"
```
2. In the beginning of the paths:
```
  /health:
    get:
      tags:
      - "health"
      summary: "retrieve Periscope server status for user"
      description: "Periscope server status."
      operationId: "getPeriscopeHealth"
      schemes:
      - "http"
      - "https"
      consumes:
      - "application/json"
      produces:
      - "application/json"
      parameters: []
      responses:
        200:
          description: "successful operation"
          schema:
            $ref: "#/definitions/Health"
      x-swagger-router-controller: "Health"
```
3. In the end of definitions:
```
definitions:
```
...
```
  Health:
    type: "object"
```
### Extend Paths
Extend all the paths: with `/api`, for example change `/v1/clusters` to `/api/v1/clusters`.

## Provide responses to all the needed Services
Introduce brand new Service for Periscope Health here as well. The new files should be [Health.js](controllers/Health.js) and [HealthService.js](controllers/HealthService.js).

For existing services here is an example for `V1alertaService`:
1. Create/Update a related `Json` file at [responses/alerts/test-alert.json](responses/alerts/test-alert.json)
2. Introduce/Update this new response to the `createMetricAlerts` method of `V1alertaService`:
```
exports.createMetricAlerts = function(args, res, next) {
  /**
   * create alert which metric based
   * Auto-scaling supports two Alert types: metric and time based. Metric based alerts are using the default (or custom) Ambari metrics. These metrics have a default Threshold value configured in Ambari - nevertheless these thresholds can be configured, changed or altered in Ambari. In order to change the default threshold for a metric please go to Ambari UI and select the Alerts tab and the metric. The values can be changed in the Threshold section. 
   *
   * clusterId Long 
   * body MetricAlertRequest  (optional)
   * returns MetricAlertResponse
   **/
  var examples = {};
  examples['application/json'] = require('../responses/alerts/test-alert.json');
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
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
docker build -t hortonworks/periscope-mock:latest .
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
