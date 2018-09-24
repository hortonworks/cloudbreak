[![CircleCI](https://circleci.com/gh/hortonworks/cb-cli.svg?style=shield)](https://circleci.com/gh/hortonworks/cb-cli) [![Go Report Card](https://goreportcard.com/badge/github.com/hortonworks/cb-cli)](https://goreportcard.com/report/github.com/hortonworks/cb-cli)

# Cloudbreak - Command Line Interface

## Install

Each Git TAG (without the leading 'v') is built and uploaded to S3 where you can download from:
```
export VERSION=2.1.0-dev.X

curl -LO "https://s3-eu-west-1.amazonaws.com/cb-cli/cb-cli_${VERSION}_Darwin_x86_64.tgz"
curl -LO "https://s3-eu-west-1.amazonaws.com/cb-cli/cb-cli_${VERSION}_Linux_x86_64.tgz"
curl -LO "https://s3-eu-west-1.amazonaws.com/cb-cli/cb-cli_${VERSION}_Windows_x86_64.tgz"
```

## Usage
To see the available commands `cb -h`.
```
NAME:
   Cloudbreak command line tool
USAGE:
   cb [global options] command [command options] [arguments...]

VERSION:
   snapshot-2017-11-28T14:44:42

AUTHOR(S):
   Hortonworks

COMMANDS:
     blueprint   blueprint related operations
     cloud       information about cloud provider resources
     cluster     cluster related operations
     configure   configure the server address and credentials used to communicate with this server
     credential  credential related operations
     ldap        ldap related operations
     recipe      recipe related operations
     help, h     Shows a list of commands or help for one command

GLOBAL OPTIONS:
   --debug        debug mode [$DEBUG]
   --help, -h     show help
   --version, -v  print the version
```
Each command provides a help flag with a description and the accepted flags and subcommands, e.g: `cb configure -h`.
```
NAME:
   Cloudbreak command line tool

USAGE:
   Cloudbreak command line tool configure [command options] [arguments...]

DESCRIPTION:
   it will save the provided server address and credential to ~/.cb/config

REQUIRED OPTIONS:
   --server value    server address [$CB_SERVER_ADDRESS]
   --username value  user name (e-mail address) [$CB_USER_NAME]

OPTIONS:
   --password value  password [$CB_PASSWORD]
   --profile value   selects a config profile to use [$CB_PROFILE]
   --output value    supported formats: json, yaml, table (default: "json") [$CB_OUT_FORMAT]
```

### Configure
Although there is an option to provide some global flags to every command to which Cloudbreak to connect to, it is recommended to save the configuration.
A configuration entry contains the Cloudbreak server's address, the username and optionally the password and the output format.
Multiple configuration profiles can be saved by specifying the `--profile` switch. The same switch can be used as a global flag to the other commands to use a specific profile.
If the profile switch is omitted, the `default` profile is saved and used.
```
cb configure --server https://ec2-52-29-224-64...compute.amazonaws.com --username your@email --password your-password --profile cloudbreak-staging
```
This will save the configuration into the user's home directory. To see its content: `cat ~/.cb/config`. If this config file is present you don't need to specify the connection flags anymore,
otherwise you need to specify these flags to every command.
```
cb cluster list --server https://ec2-52-29-224-64...compute.amazonaws.com --username your@email --password your-password
```

### Create cluster
To create a cluster with the CLI, a cluster descriptor file needs to be put together and specified as an input to the `cluster create` command:
```
cb cluster create --cli-input-json create_cluster.json
```

The cluster descriptor is basically the JSON request that's being sent to the Cloudbreak API.
The full reference of this descriptor file can be found in the API docs.
The CLI can help with creating the skeleton of the cluster descriptor JSON.
The following command outputs a descriptor file with empty values:
```
cb cluster generate-template aws existing-subnet --blueprint-name "my-custom-blueprint"
```
The `aws` and `existing-subnet` keywords are subcommands to the `cluster generate-template` command and help with creating a skeleton with proper entries for the selected cloud provider and network configuration.
Use the `-h` option to see the available subcommands, e.g.:
```
cb cluster generate-template -h
```
Direct the output to a file to save the skeleton locally.
```
cb cluster generate-template aws existing-subnet > create_cluster.json
```
To create a cluster, fill the empty values or change the existing ones and use the `create-cluster` command above.

### Terminate cluster
To terminate the previously created cluster use the `delete-cluster` command.
```
cb cluster delete --cluster-name my-cluster
```

### Describe cluster
If you want to check out the properties of a running cluster the `describe-cluster` command can be useful.
```
cb cluster describe --cluster-name my-cluster
```

## Bash/Zsh autocompletion
To enable autocompletion on bash or zsh source the appropriate file under the `autocomplete` folder:
```
source autocomplete/bash_autocomplete
```
On linux copy the file into `/etc/bash_completion.d/cb`. Don't forget to source the file to make it active in the current shell.

## Debug
To enable the debug logging use the `--debug` global switch
```
cb --debug cluster list
```
or provide it as an environment variable `export DEBUG=1` or inline
```
DEBUG=1 cb clusters list
```

## Proxy settings
To use the cb cli behind a proxy you must use the following environment variable:
```
 export HTTP_PROXY=10.0.0.133:3128
```
or with basic auth:
```
export HTTP_PROXY=http://user:pass@10.0.0.133:3128/
```

## Dependency management

This project uses Go [modules](https://github.com/golang/go/wiki/Modules) for dependency management, introduced in golang 1.11. The Makefile enables module support explicitly on build goals, but you can also choose enable it in your system, by setting:
```
export GO111MODULE=on
```

## Implementing new commands
Top level commands like `cluster` are separated into a cmd package. You can see an example for these commands in the `cloudbreak/cmd` folder. Each of these resource separated files contain an `init` function that adds the resource specific commands to an internal array:
```$xslt
func init() {
    CloudbreakCommands = append(CloudbreakCommands, cli.Command{})
}
```
The `init()` function is automatically invoked for each file in the `cmd` folder, because it is referenced from the `main.go` file:
```$xslt
import (
	"github.com/hortonworks/cb-cli/cloudbreak/cmd"
)
```
and then added to the main app:
```$xslt
app.Commands = append(app.Commands, cmd.CloudbreakCommands...)
```
To implement new top level commands you can create your own folder structure and reproduce the above specified:
```$xslt
- Create your own resource separation files/folders etc..
- Implement the init() function for these files
- Reference these files from the main.go file
- Add your commands to the app.Commands
```
If you'd like to introduce `sub-commands` for already existing top level commands that are not Cloudbreak specific you can move the `cluster.go` file (for example) from
the `cloudbreak/cmd` folder to a top level `cmd` folder and reference it from the `main.go` file as describe above.

### Plugins
The CLI also supports a `plugin` model. This means that you can create similar CLI tools like this one and build them independently/separately, but get them invoked by the main CLI tool. Let's assume you'd like to create a `dlm` CLI, but develop it outside from this repository, but with the same framework.
If you create the `dlm` binary and put it in your `$PATH` you can invoke with the main CLI tool like:
```$xslt
cb dlm my-command
```
In order to do this the main CLI needs to be built with plugin mode enabled:
```$xslt
PLUGIN_ENABLED=true make build
```
This way you have introduced another top level command, but you have the advantage to:
```$xslt
* dynamically install/enable/disable top level commands without re-building/downloading the top level CLI
* dynamically upgrade commands without re-building/downloading the top level CLI
* develop it independently from this repository
* have an independent CLI tool that can be invoked without the top level CLI
```