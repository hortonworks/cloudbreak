# Cloudbreak API mock

## Get SWAGGER Codegen

Install [Swagger Codegen CLI](https://github.com/swagger-api/swagger-codegen#prerequisites). 

CLI is also available as [Docker Image](https://hub.docker.com/r/swaggerapi/swagger-codegen-cli/). You can read further [here](https://github.com/swagger-api/swagger-codegen#swagger-codegen-cli-docker-image).

### Get Help
To get a list of available general options, you can run `help generate`.

### NodeJS specific options
Supported config options can be different per language. Running `config-help -l {lang}` will show available options.

## Server stub generator HOWTO
[The documentation](https://github.com/swagger-api/swagger-codegen/wiki/Server-stub-generator-HOWTO) to generate a server stub for a couple different frameworks.

## Generate Cloudbreak API NodeJS Client
```
make generate-api
```

## Upgrade Swagger YML

### Base Path
Change `basePath: "/api"` to `basePath: "/cb"` at [/api/swagger.yaml](api/swagger.yaml).

### New Info and Health Services
Introduce brand new Services for Cloudbreak Info and Health.

1. In the `tags` section:
```
tags:
- name: "info"
- name: "health"
```
2. In the beginning of the `paths`:
```
paths:
  /info:
    get:
      tags:
      - "info"
      summary: "retrieve Cloudbreak version for user"
      description: "Cloudbreak version information."
      operationId: "getCloudbreakInfo"
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
            $ref: "#/definitions/Info"
      x-swagger-router-controller: "Info"
  /health:
    get:
      tags:
      - "health"
      summary: "retrieve Cloudbreak server status for user"
      description: "Cloudbreak server status."
      operationId: "getCloudbreakHealth"
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
            $ref: "#/definitions/Info"
      x-swagger-router-controller: "Info"
```
3. In the end of `definitions`:
```
definitions:
```
...
```
  Info:
    type: "object"
```

### Extend Paths
Extend all the `paths:` with `/api`, for example change `/v1/accountpreferences/isplatformselectiondisabled` to `/api/v1/accountpreferences/isplatformselectiondisabled`

## Provide responses to all the needed Services
Introduce brand new Services for Cloudbreak Info and Health here as well. The new files should be [Info.js](controllers/Info.js) and [InfoService.js](service/InfoService.js).

For existing services here is an example for `V1credentialsService`:
1. Create/Update a related `Json` file at [responses/credentials/openstack.json](responses/credentials/openstack.json)
2. Introduce/Update this new response to the `getPublicsCredential` method of `V1credentialsService`:


For existing services here is an example for `V1credentialsService.js`:
```
exports.getPublicsCredential = function(args, res, next) {
  /**
   * retrieve public and private (owned) credentials
   * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
   *
   * returns List
   **/
  var openstack_data = require('../responses/credentials/openstack.json');
  var aws_data = require('../responses/credentials/aws.json');
  var azure_data = require('../responses/credentials/azure.json');
  var gcp_data = require('../responses/credentials/gcp.json');
  var response_array = [];

  response_array.push(openstack_data,aws_data,azure_data,gcp_data);

  var examples = {};
  examples['application/json'] = response_array;
  if (Object.keys(examples).length > 0) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(examples[Object.keys(examples)[0]] || {}, null, 2));
  } else {
    res.end();
  }
}
```

## Cloudbreak API Mock Docker Image
You can find at [Docker Hub](https://hub.docker.com/r/hortonworks/cloudbreak-mock/)

### Build local image
You can build your own Cloudbreak Mock image locally with:
```
docker build -t hortonworks/cloudbreak-mock:latest .
```

## Update CBD Profile with Cloudbreak API Mock Image
First, initialize Cloudbreak Mock by updating the CBD Profile file with the following content:
```
export DOCKER_IMAGE_CLOUDBREAK=hortonworks/cloudbreak-mock
export DOCKER_TAG_CLOUDBREAK=latest
```

## Start Cloudbreak Mock
To start the Cloudbreak Mock application use the following command:
```
cbd start
```
This will start all the needed Docker containers and initialize the mock application.
