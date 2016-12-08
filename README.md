# Hortonworks Data Cloud - Command Line Interface

## Install

### Private
Since the repository is private you can only download it by going to the releases page and download the desired version
or by creating a GitHub personal access token on your settings page and then:
```
export GITHUB_TOKEN=YOUR_TOKEN
export VERSION=1.6.0

 curl -s -G \
   -d access_token=$GITHUB_TOKEN \
   https://api.github.com/repos/hortonworks/hdc-cli/releases/tags/v$VERSION \
    | jq ".assets[]|[.name,.url][]" -r \
    | xargs -t -n 2 -P 3 curl -sG -d access_token=$GITHUB_TOKEN -H "Accept: application/octet-stream" -Lo
```

### Public 
This method only works if the GitHub repository is public which is not the case ATM so ignore it.

#### OSX, Linux
You can install directly the GitHub binary release:
```
curl -LsS https://github.com/hortonworks/hdc-cli/releases/download/v1.6.0/hdc-cli_1.6.0-rc.1_$(uname)_x86_64.tgz | sudo tar -zx -C /usr/local/bin
```
#### Windows
The windows binary is in experimental phase, but you can download it from the GitHub release page.
```
﻿C:\Users\IEUser\Desktop>hdc.exe terminate-cluster --cluster-name my-cluster
﻿ERROR: [TerminateCluster] (status 404): Stack 'my-cluster' not found
```

## Usage
To see the available commands `hdc -h`.
```
NAME:
   hdc - Hortonworks Data Cloud command line tool

USAGE:
   hdc [global options] command [command options] [arguments...]

VERSION:
   1.6.0-rc.1-2016-09-13T09:21:52

AUTHOR(S):
   Hortonworks

COMMANDS:
     configure           configure the server address and credentials used to communicate with this server
     create-cluster      creates a new cluster
     describe-cluster    get a detailed description of a cluster
     list-cluster-types  list available cluster types and HDP versions
     list-clusters       list available clusters
     resize-cluster      change the number of worker nodes of an existing cluster
     terminate-cluster   terminates a cluster
     help, h             Shows a list of commands or help for one command

GLOBAL OPTIONS:
   --debug        debug mode [$DEBUG]
   --help, -h     show help
   --version, -v  print the version
```
Each command provides a help flag with a description and the accepted flags and subcommands, e.g: `hdc configure -h`.
```
NAME:
   hdc configure - configure the server address and credentials used to communicate with this server

USAGE:
   hdc configure [command options] [arguments...]

DESCRIPTION:
   it will save the provided server address and credential to /root/.hdc/config

REQUIRED OPTIONS:
   --server value    server address [$CB_SERVER_ADDRESS]
   --username value  user name (e-mail address) [$CB_USER_NAME]
   --password value  password [$CB_PASSWORD]

OPTIONS:
   --output value  supported formats: json, yaml, table (default: "json") [$CB_OUT_FORMAT]
```

### Configure
Although there is an option to provide some global flags to every command to which Cloudbreak to connect to, it is recommended to save the configuration. 
```
hdc configure --server https://ec2-52-29-224-64...compute.amazonaws.com --username your@email --password your-password
```
This will save the configuration into the user's home directory. To see its content: `cat ~/.hdc/config`. If this config file is present you don't need to specify the connection flags anymore,
otherwise you need to specify these flags to every command.
```
hdc list-clusters --server https://ec2-52-29-224-64...compute.amazonaws.com --username your@email --password your-password
```

### Create cluster
Most commands have a sub command called `generate-cli-skeleton` and `--cli-input-json` parameter that you can use to store parameters in JSON and read them from a file instead of typing them at the command line.
```
hdc create-cluster generate-cli-skeleton
```
Direct the output to a file to save the skeleton locally.
```
hdc create-cluster generate-cli-skeleton > create_cluster.json
```
To get a detailed explanation of the parameters use the `help` command.
```
hdc create-cluster generate-cli-skeleton --help
```
To create a cluster fill the empty values or change the existing ones, e.g:
```
{
  "ClusterName": "my-cluster",
  "HDPVersion": "2.5",
  "ClusterType": "EDW-ETL: Apache Spark 2.0-preview, Apache Hive 2.0",
  "Master": {
    "InstanceType": "m4.4xlarge",
    "VolumeType": "gp2",
    "VolumeSize": 32,
    "VolumeCount": 1
  },
  "Worker": {
    "InstanceType": "m3.xlarge",
    "VolumeType": "ephemeral",
    "VolumeSize": 40,
    "VolumeCount": 2,
    "InstanceCount": 2
  },
  "Compute": {
    "InstanceType": "m3.xlarge",
    "VolumeType": "ephemeral",
    "VolumeSize": 40,
    "VolumeCount": 2,
    "InstanceCount": 0
  },
  "SSHKeyName": "my-existing-keypair-name",
  "RemoteAccess": "0.0.0.0/0",
  "WebAccess": true,
  "ClusterAndAmbariUser": "admin",
  "ClusterAndAmbariPassword": "admin"
}

```
and pass the JSON configuration to the `--cli-input-json` parameter.
```
hdc create-cluster --cli-input-json create_cluster.json
```

### Terminate cluster
To terminate the previously created cluster use the `terminate-cluster` command.
```
hdc terminate-cluster --cluster-name my-cluster
```

### Describe cluster
If you want to clone a cluster the `describe-cluster` command can be useful.
```
hdc describe-cluster --cluster-name my-cluster

{
  "ClusterName": "my-cluster",
  "HDPVersion": "2.5",
  "ClusterType": "EDW-ETL: Apache Spark 2.0-preview, Apache Hive 2.0",
  "Master": {
    "InstanceType": "m4.4xlarge",
    "VolumeType": "gp2",
    "VolumeSize": 32,
    "VolumeCount": 1
  },
  "Worker": {
    "InstanceType": "m3.xlarge",
    "VolumeType": "ephemeral",
    "VolumeSize": 40,
    "VolumeCount": 2,
    "InstanceCount": 2
  },
  "Compute": {
    "InstanceType": "m3.xlarge",
    "VolumeType": "ephemeral",
    "VolumeSize": 40,
    "VolumeCount": 2,
    "InstanceCount": 0
  },
  "SSHKeyName": "my-existing-keypair-name",
  "RemoteAccess": "0.0.0.0/0",
  "WebAccess": true,
  "ClusterAndAmbariUser": "admin",
  "ClusterAndAmbariPassword": "",
  "Status": "CREATE_IN_PROGRESS",
  "StatusReason": "Creating infrastructure"
}

```

## Debug
To enable the debug logging use the `--debug` global switch
```
hdc --debug list-clusters
```
or provide it as an environment variable `export DEBUG=1` or inline
```
DEBUG=1 hdc list-clusters
```

## Proxy settings
To use the hdc cli behind a proxy you must use the following environment variable:
```
 export HTTP_PROXY=10.0.0.133:3128
```
or with basic auth:
```
export HTTP_PROXY=http://user:pass@10.0.0.133:3128/
```

## Dependency management

This project uses [Godep](https://github.com/tools/godep) for dependency management. You can install it and download the configured dependency versions with:
```
go get github.com/tools/godep
godep restore
```

If you changed the external dependencies then you can save it with:
```
godep save
```
