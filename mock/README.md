# Cloudbreak API mock

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
   -i https://qa-cloudbreak.eng.hortonworks.com/cb/api/swagger.json \
   -l nodejs-server \
   -o cbd-mock/javascript_api_client \
   -Dio.swagger.parser.util.RemoteUrl.trustAll=true \
   -Dapis -DapiTests=false
```

## Server stub generator HOWTO
[The documentation](https://github.com/swagger-api/swagger-codegen/wiki/Server-stub-generator-HOWTO) to generate a server stub for a couple different frameworks.

## Download Cloudbreak SWAGGER JSON
```curl --insecure https://qa-cloudbreak.eng.hortonworks.com/cb/api/swagger.json --output swagger.json```

## Generate Cloudbreak API NodeJS Client
```
java -jar /usr/local/Cellar/swagger-codegen/2.2.3/libexec/swagger-codegen-cli.jar generate \
   -i swagger.json \
   -l nodejs-server \
   -o javascript_api_client
```

* `/usr/local/Cellar/swagger-codegen/2.2.3/libexec/swagger-codegen-cli.jar`: the Swagger Codegen CLI path on OS X. This can be vary on different OSs and with different installs. You can read more about this at [Swagger Codegen CLI Installation](https://github.com/swagger-api/swagger-codegen#table-of-contents).
* `javascript_api_client`: the destination folder for the generated client. This is also can be vary based on your decision. 

## Fix basePath in the generated Swagger YML
Change `basePath: "/api"` to `basePath: "/cb/api"` at [/api/swagger.yaml](api/swagger.yaml).

## Provide responses to all the needed Services
Here is an example for `V1credentialsService.js`:
```
exports.getPublicsCredential = function(args, res, next) {
  /**
   * retrieve public and private (owned) credentials
   * Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak does not store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak deployer is purely acting on behalf of the end user - without having access to the user's account.
   *
   * returns List
   **/
  var examples = {};
  examples['application/json'] = 
  [
    {
      "name":"openstack",
      "cloudPlatform":"OPENSTACK",
      "parameters":
      {
        "facing":"internal",
        "endpoint":"http://openstack.eng.com:3000/v2.0",
        "selector":"cb-keystone-v2",
        "keystoneVersion":"cb-keystone-v2",
        "userName":"cloudbreak",
        "tenantName":"cloudbreak"
      },
      "description":"",
      "topologyId":null,
      "id":1,
      "public":false
    },{
      "name":"azure",
      "cloudPlatform":"AZURE",
      "parameters":
      {
        "tenantId":"a12b1234-1234-12aa-3bcc-4d5e6f78900g",
        "spDisplayName":null,
        "subscriptionId":"a12b1234-1234-12aa-3bcc-4d5e6f78900g",
        "roleType":null,
        "accessKey":"a12b1234-1234-12aa-3bcc-4d5e6f78900g"
      },
      "description":"",
      "topologyId":null,
      "id":2,
      "public":false
    },{
      "name":"google",
      "cloudPlatform":"GCP",
      "parameters":
      {
        "serviceAccountId":"1234567890-abcde1fghijk2lmn1o2p34q5r7stuvz@developer.gserviceaccount.com",
        "projectId":"cloudbreak"
      },
      "description":"",
      "topologyId":null,
      "id":3,
      "public":false
    },{
      "name":"amazon",
      "cloudPlatform":"AWS",
      "parameters":
      {
        "smartSenseId":"null",
        "selector":"role-based"
      },
      "description":"",
      "topologyId":null,
      "id":4,
      "public":false
    }
  ];
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
docker build -t hortonworks/cloudbreak-mock .
```

## Update CBD Profile with Cloudbreak API Mock Image
First, initialize Cloudbreak Mock by updating the CBD Profile file with the following content:
```
export DOCKER_IMAGE_CLOUDBREAK=hortonworks/cloudbreak-mock
```

## Start Cloudbreak Mock
To start the Cloudbreak Mock application use the following command:
```
cbd start
```
This will start all the needed Docker containers and initialize the mock application.
