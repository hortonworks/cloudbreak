# Tests with mock clusters

This readme will guide you through two topics: 
* section "Create mock clusters"
* section "Performance tests"

Both of them need similar preparation, so you will find first a section "Common steps" that explains how to set up either of them.

## Common steps

### Edit /etc/hosts file

Address `mockhosts.service.consul` has to be resolvable from the host where perftest will be running.

Edit your `/etc/hosts` file and add `mockhosts.service.consul` as can be seen below:

```
##
# Host Database
#
# localhost is used to configure the loopback interface
# when the system is booting.  Do not change this entry.
##
127.0.0.1       localhost       mockhosts.service.consul
255.255.255.255 broadcasthost
::1             localhost
```

### Start mockSparkServer
First, you have to start the mock spark server:

#### IDEA
Start class mockSparkServerApplication with following environment parameter:

```
CB_SERVER_ADDRESS=192.168.99.100
```

#### Gradle

Run the following gradle command:
````
CB_SERVER_ADDRESS=$CB_PERFTEST_HOST MOCK_SERVER_ADDRESS=$CB_MOCK_HOST ./gradlew :integration-test:runMockServer
````

Where:
* `CB_PERFTEST_HOST`: equals to the value of variable `YOUR_IP` from your environment folder / Profile file. It is 192.168.99.100 by default.
* `MOCK_SERVER_ADDRESS`: is equal to `mockhosts.service.consul`

## Creating mock clusters

You can create mock clusters by using below V2 cluster request json and blueprint.

### With blueprint

Please add following blueprint to cloudbreak with name mockblueprint (will be referenced from V2 cluster request).

```json
{
  "configurations": [],
  "host_groups": [
    {
      "name": "master",
      "components": [
        {
          "name": "NAMENODE"
        },
        {
          "name": "SECONDARY_NAMENODE"
        },
        {
          "name": "RESOURCEMANAGER"
        },
        {
          "name": "APP_TIMELINE_SERVER"
        },
        {
          "name": "HISTORYSERVER"
        },
        {
          "name": "ZOOKEEPER_SERVER"
        }
      ],
      "cardinality": "1"
    },
    {
      "name": "slave1",
      "components": [
        {
          "name": "DATANODE"
        },
        {
          "name": "HDFS_CLIENT"
        },
        {
          "name": "NODEMANAGER"
        },
        {
          "name": "YARN_CLIENT"
        },
        {
          "name": "MAPREDUCE2_CLIENT"
        },
        {
          "name": "ZOOKEEPER_CLIENT"
        }
      ],
      "cardinality": "2"
    }
  ],
  "Blueprints": {
    "blueprint_name": "multi-node-hdfs-yarn",
    "stack_name": "HDP",
    "stack_version": "2.4"
  }
}
```

### With V2 cluster request json

Please run cb with following V2 cluster request to create a cluster.

```json
{
  "inputs": {},
  "general": {
    "credentialName": "mockcredential",
    "name": "mockcluster"
  },
  "placement": {
    "region": "us-west-2",
    "availabilityZone": "us-west-2a"
  },
    "imageSettings": {
	"imageCatalog":"mockcatalog",
	"imageId":"aea6c9e1-f775-47f2-574b-c320504c1e02",
	"os":"Amazonlinux"
        },
  "tags": {
    "userDefinedTags": {}
  },
  "cluster": {
    "ambari": {
      "blueprintName": "mockblueprint",
      "platformVersion": "HDP 2.6",
      "gateway": {
        "enableGateway": true,
        "topologies": [
          {
            "topologyName": "dp-proxy",
            "exposedServices": [
              "AMBARI"
            ]
          }
        ],
        "ssoType": "NONE"
      },
      "userName": "admin",
      "password": "password",
      "validateBlueprint": false,
      "ambariSecurityMasterKey": "",
      "enableSecurity": false
    }
  },
  "instanceGroups": [
    {
      "parameters": {},
      "template": {
        "parameters": {},
        "instanceType": "m5.2xlarge",
        "volumeType": "ssd",
        "volumeCount": 1,
        "volumeSize": 100,
        "rootVolumeSize": 50,
        "awsParameters": {
          "encryption": {
            "type": "NONE"
          }
        }
      },
      "nodeCount": 1,
      "group": "master",
      "type": "GATEWAY",
      "recoveryMode": "MANUAL",
      "securityGroup": {
        "securityRules": [
          {
            "subnet": "0.0.0.0/0",
            "ports": "9443",
            "protocol": "tcp"
          },
          {
            "subnet": "0.0.0.0/0",
            "ports": "22",
            "protocol": "tcp"
          },
          {
            "subnet": "0.0.0.0/0",
            "protocol": "tcp",
            "ports": "8443"
          }
        ]
      }
    },
    {
      "parameters": {},
      "template": {
        "parameters": {},
        "instanceType": "m5.xlarge",
        "volumeType": "ssd",
        "volumeCount": 1,
        "volumeSize": 100,
        "rootVolumeSize": 50,
        "awsParameters": {
          "encryption": {
            "type": "NONE"
          }
        }
      },
      "nodeCount": 1,
      "group": "slave1",
      "type": "CORE",
      "recoveryMode": "MANUAL",
      "securityGroup": {
        "securityRules": [
          {
            "subnet": "0.0.0.0/0",
            "protocol": "tcp",
            "ports": "22"
          }
        ]
      }
    }
  ],
  "network": {
    "parameters": {},
    "subnetCIDR": "10.0.0.0/16"
  },
  "stackAuthentication": {
    "publicKeyId": "publickey-name"
  }
}
```

### Number of nodes

If you want to increase number of nodes per cluster, then 

1) edit `NUMBER_OF_INSTANCES` in class MockSparkServer,
2) restart mockSparkServer 
3) edit nodeCount in the V2 cluster request json (nodes from all groups has to be equal to the value of `NUMBER_OF_INSTANCES`)
 
## Performance test

### admin@example.com in UAA

User admin@example.com should exist in UAA. To add it: 

1) edit uaa.yml in your environment folder (where your Profile file is located), add admin@example.com with admin priviliges
2) restart uaa container

### Running existing tests

Now you just have to start `perftest.sh` in `performance-test`.

If everything goes well, you will see a sea of log lines. When the simulation finishes, your output should be similar to this:

````
'================================================================================
---- Global Information --------------------------------------------------------
> request count                                         12 (OK=12     KO=0     )
> min response time                                     42 (OK=42     KO=-     )
> max response time                                   2715 (OK=2715   KO=-     )
> mean response time                                   372 (OK=372    KO=-     )
> std deviation                                        717 (OK=717    KO=-     )
> response time 50th percentile                        122 (OK=122    KO=-     )
> response time 75th percentile                        243 (OK=243    KO=-     )
> response time 95th percentile                       1478 (OK=1478   KO=-     )
> response time 99th percentile                       2468 (OK=2468   KO=-     )
> mean requests/sec                                  0.146 (OK=0.146  KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                            11 ( 92%)
> 800 ms < t < 1200 ms                                   0 (  0%)
> t > 1200 ms                                            1 (  8%)
> failed                                                 0 (  0%)
================================================================================
````  

### Results

Results can be appreciated in `performance-test/results/cloudbreaksimulation-TIMESTAMP`, where TIMESTAMP is a unix timestamp expressed in ms. There is an index.html, reporting success / failure, as well as all kinds of statistics.

There is also a `simulation.log` file that contains all the executed queries and their result.

### Parameters

In perftest.sh you can fiddle with following parameters:

* CB_NUMBER_OF_USERS: number of concurrent users. 
* CB_RAMPUP_SECONDS: See [Injection profile](https://gatling.io/docs/2.3/general/simulation_setup/#injection). We currently use rampUsers(CB_NUMBER_OF_USERS) over (RAMPUP_SECONDS)

### Troubleshooting

In some cases there are failures to execute one or more steps. If gatling cannot get beyond a specific step, it just will execute it over and over again - thus goes to an infinite loop. 
Intermediate statistics pages help to understand what is failing:

````
`================================================================================
2018-10-02 07:24:36                                        1150s elapsed
---- Requests ------------------------------------------------------------------
> Global                                                   (OK=1283   KO=110   )
> uaa token request                                        (OK=16     KO=0     )
> query cb version                                         (OK=16     KO=0     )
> create mock imagecatalog                                 (OK=16     KO=0     )
> query credentials                                        (OK=16     KO=0     )
> create blueprint                                         (OK=16     KO=0     )
> create mock credential                                   (OK=16     KO=0     )
> create mock stack v2                                     (OK=16     KO=0     )
> delete stack                                             (OK=0      KO=16    )
> get stack                                                (OK=1165   KO=94    )
> delete blueprint                                         (OK=2      KO=0     )
> delete credential                                        (OK=2      KO=0     )
> delete imagecatalog                                      (OK=2      KO=0     )
---- Errors --------------------------------------------------------------------
> status.find.is(200), but actually found 400                        50 (45.45%)
> status.find.is(200), but actually found 403                        20 (18.18%)
> j.u.c.TimeoutException: Request timeout to /192.168.99.100:443     16 (14.55%)
 after 60000 ms
> status.find.is(200), but actually found 500                        15 (13.64%)
> j.n.ConnectException: handshake timed out                           9 ( 8.18%)

---- cluster creation v2 -------------------------------------------------------
[#########-----------------------------------------------------------------] 12%
          waiting: 0      / active: 14     / done:2
================================================================================
````

Also, you can check the simulation.log (see section "Results") to see which requests went wrong. 

It might be useful to save all output of the simulation to file as failed responses are logged.

### Further development

Perftest make use of gatling: [Gatling documentation](https://gatling.io/documentation/).

In short, tests are organized into simulations. Simulations are written in a scala-based DSL. 