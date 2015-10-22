## Cloudbreak Shell

The goal with the CLI was to provide an interactive command line tool which supports:

* all functionality available through the REST API or Cloudbreak web UI
* makes possible complete automation of management task via **scripts**
* context aware command availability
* tab completion
* required/optional parameter support
* **hint** command to guide you on the usual path

## Install and start Cloudbreak shell

You have a few options to give it a try:

- [use Cloudreak deployer - **recommended**](#deployer)
- [use our prepared docker image](#dockerimage)
- [build it from source](#fromsource)

<a name="deployer"></a>
### Starting cloudbreak shell using cloudbreak deployer

Start the shell with `cbd util cloudbreak-shell`. This will launch the Cloudbreak shell inside a Docker container and you are ready to start using it.

<a name="dockerimage"></a>
### Starting Cloudbreak shell with our prepared docker image

You can find the docker image and its documentation [here](https://github.com/sequenceiq/docker-cb-shell).

<a name="fromsource"></a>
### Build from source

If want to use the code or extend it with new commands follow the steps below. You will need:
- jdk 1.7

```
git clone https://github.com/sequenceiq/cloudbreak-shell.git
cd cloudbreak-shell
./gradlew clean build
```

_Note: In case you use the hosted version of Cloudbreak you should use the `latest-release.sh` to get the right version of the CLI._

**Start Cloudbreak-shell from the built source**

```
Usage:
  java -jar cloudbreak-shell-0.5-SNAPSHOT.jar                  : Starts Cloudbreak Shell in interactive mode.
  java -jar cloudbreak-shell-0.5-SNAPSHOT.jar --cmdfile=<FILE> : Cloudbreak executes commands read from the file.

Options:
  --cloudbreak.address=<http[s]://HOSTNAME:PORT>  Address of the Cloudbreak Server [default: https://cloudbreak-api.sequenceiq.com].
  --identity.address=<http[s]://HOSTNAME:PORT>    Address of the SequenceIQ identity server [default: https://identity.sequenceiq.com].
  --sequenceiq.user=<USER>                        Username of the SequenceIQ user [default: user@sequenceiq.com].
  --sequenceiq.password=<PASSWORD>                Password of the SequenceIQ user [default: password].

Note:
  You should specify at least your username and password.
```
Once you are connected you can start to create a cluster. If you are lost and need guidance through the process you can use `hint`. You can always use `TAB` for completion. Note that all commands are `context aware` - they are available only when it makes sense - this way you are never confused and guided by the system on the right path.

You can find the provider specific documentations here:

- [AWS](aws_cb_shell.md)
- [Azure](azure_cb_shell.md)
- [GCP](gcp_cb_shell.md)
- [OpenStack](openstack_cb_shell.md)

