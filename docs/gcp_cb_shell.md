## Interactive mode

Start the shell with `cbd util cloudbreak-shell`. This will launch the Cloudbreak shell inside a Docker container and you are ready to start using it.

You have to copy files into the cbd working directory, which you would like to use from shell. For example if your `cbd` working directory is `~/prj/cbd` then copy your blueprint and public ssh key file into this directory. You can refer to these files with their names from the shell.

### Create a cloud credential

In order to start using Cloudbreak to provision a cluster in Google Cloud you will need to have a GCP credential. If you do not want to Cloubreak to reach your Google Cloud resources then you have to delete the service account.
```
credential create --GCP --description "short description of your linked credential" --name my-gcp-credential --projectId <your gcp projectid> --serviceAccountId <your GCP service account mail address> --serviceAccountPrivateKeyPath <path of your GCP service account generated private key> --sshKeyPath <path of your GCP public key>
```

Alternatively you can upload your public key from an url as well, by using the `—sshKeyUrl` switch. You can check whether the credential was creates successfully by using the `credential list` command.
You can switch between your cloud credential - when you’d like to use one and act with that you will have to use:
```
credential select --name my-gcp-credential
```

You can delete your cloud credential - when you’d like to delete one you will have to use:
```
credential delete --name my-gcp-credential
```

You can show your cloud credential - when you’d like to show one you will have to use:
```
credential show --name my-gcp-credential
```

### Create a template

A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. A template can be used repeatedly to create identical copies of the same stack (or to use as a foundation to start a new stack).

```
template create --GCP --name gcptemplate --description gcp-template --instanceType N1_STANDARD_4 --volumeSize 100 --volumeCount 2
```
Other available options:

`--volumeType` defaults to "HDD", other allowed value: "SSD"

`--publicInAccount` flags if the template is public in the account

You can check whether the template was created successfully by using the `template list` or `template show` command.

You can delete your cloud template - when you’d like to delete one you will have to use:
```
template delete --name gcptemplate
```

### Create or select a blueprint

You can define Ambari blueprints with cloudbreak-shell:

```
blueprint add --name myblueprint --description myblueprint-description --file <the path of the blueprint>
```

Other available options:

`--url` the url of the blueprint

`--publicInAccount` flags if the network is public in the account

We ship default Ambari blueprints with Cloudbreak. You can use these blueprints or add yours. To see the available blueprints and use one of them please use:

```
blueprint list

blueprint select --name hdp-small-default
```

### Create a network

A network gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related networking, maintaining and updating them in an orderly and predictable fashion. A network can be used repeatedly to create identical copies of the same stack (or to use as a foundation to start a new stack).

```
network create --GCP --name gcpnetwork --description "gcp network"--subnet 10.0.0.0/16
```

Other available options:

`--publicInAccount` flags if the network is public in the account

There is a default network with name `default-gcp-network`. If we use this for cluster creation, Cloudbreak will create the cluster within the 10.0.0.0/16 subnet.

You can check whether the network was created successfully by using the `network list` command. Check the network and select it if you are happy with it:

```
network show --name gcpnetwork

network select --name gcpnetwork
```

### Create a security group

A security group gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related security rules.

```
securitygroup create --name secgroup_example --description securitygroup-example --rules 0.0.0.0/0:tcp:8080,9090;10.0.33.0/24:tcp:1234,1235
```

You can check whether the security group was created successfully by using the `securitygroup list` command. Check the security group and select it if you are happy with it:

```
securitygroup show --name secgroup_example

securitygroup select --name secgroup_example
```

There are two default security groups defined: `all-services-port` and `only-ssh-and-ssl`

`only-ssh-and-ssl:` all ports are locked down (you can't access Hadoop services outside of the VPC)

* SSH (22)
* HTTPS (443)

`all-services-port:` all Hadoop services + SSH/HTTP are accessible by default:

* SSH (22)
* HTTPS (443)
* Ambari (8080)
* Consul (8500)
* NN (50070)
* RM Web (8088)
* Scheduler (8030RM)
* IPC (8050RM)
* Job history server (19888)
* HBase master (60000)
* HBase master web (60010)
* HBase RS (16020)
* HBase RS info (60030)
* Falcon (15000)
* Storm (8744)
* Hive metastore (9083)
* Hive server (10000)
* Hive server HTTP (10001)
* Accumulo master (9999)
* Accumulo Tserver (9997)
* Atlas (21000)
* KNOX (8443)
* Oozie (11000)
* Spark HS (18080)
* NM Web (8042)
* Zeppelin WebSocket (9996)
* Zeppelin UI (9995)
* Kibana (3080)
* Elasticsearch (9200)

### Configure instance groups

You have to configure the instancegroups before the provisioning. An instancegroup is defining a group of your nodes with a specified template. Usually we create instancegroups for the hostgroups defined in the blueprints.
```
instancegroup configure --instanceGroup host_group_slave_1 --nodecount 3 --templateName minviable-gcp
```

Other available options:

`--templateId` Id of the template

### Create a Hadoop cluster
You are almost done - two more command and this will create your Hadoop cluster on your favorite cloud provider. Same as the API, or UI this will use your `credential`, `instancegroups`, `network`, `securitygroup`, and by using Google Cloud Platform will launch a cloud stack
```
stack create --name my-first-stack --region US_CENTRAL1_A
```
Once the `stack` is up and running (cloud provisioning is done) it will use your selected `blueprint` and install your custom Hadoop cluster with the selected components and services.
```
cluster create --description "my first cluster"
```
You are done - you can check the progress through the Ambari UI. If you log back to Cloudbreak UI you can check the progress over there as well, and learn the IP address of Ambari.

### Stop/Restart cluster and stack
You have the ability to **stop your existing stack then its cluster** if you want to suspend the work on it.

Select a stack for example with its name:
```
stack select --name my-stack
```
Other available option to define a stack is its `--id` (instead of the `--name`).

Apply the following commands to stop the previously selected stack:
```
cluster stop
stack stop
```
>**Important!** The related cluster should be stopped before you can stop the stack.


Apply the following command to **restart the previously selected and stopped stack**:
```
stack start
```
After the selected stack has restarted, you can **restart the related cluster as well**:
```
cluster start
```

### Upscale/Downscale cluster and stack
You can **upscale your selected stack** if you need more instances to your infrastructure:
```
stack node --ADD --instanceGroup host_group_slave_1 --adjustment 2
```
Other available option `--withClusterUpScale`. Its boolean value indicates the cluster upscale as well after the 
stack upscale.

You can upscale the related cluster separately as well:
```
cluster node --ADD --hostgroup host_group_slave_1 --adjustment 2
```


Apply the following command to **downscale the previously selected stack**:
```
stack node --REMOVE  --instanceGroup host_group_slave_1 --adjustment -2
```
and the related cluster separately:
```
cluster node --REMOVE  --hostgroup host_group_slave_1 --adjustment -2
```

## Silent mode

With Cloudbreak shell you can execute script files as well. A script file contains cloudbreak shell commands and can be executed with the `script` cloudbreak shell command

```
script <your script file>
```

or with the `cbd util cloudbreak-shell-quiet` cbd command:

```
cbd util cloudbreak-shell-quiet < example.sh
```

## Example

The following example creates a hadoop cluster with `hdp-small-default` blueprint on M3Xlarge instances with 2X100G attached disks on `default-gcp-network` network using `all-services-port` security group. You should copy your ssh public key file and your GCP service account generated private key into your cbd working directory with name `id_rsa.pub` and `gcp.p12` and change the `<...>` parts with your gcp credential details.

```
credential create --GCP --description "my credential" --name my-gcp-credential --projectId <your gcp projectid> --serviceAccountId <your GCP service account mail address> --serviceAccountPrivateKeyPath gcp.p12 --sshKeyFile id_rsa.pub
credential select --name my-gcp-credential
template create --GCP --name gcptemplate --description gcp-template --instanceType N1_STANDARD_4 --volumeSize 100 --volumeCount 2
blueprint select --name hdp-small-default
instancegroup configure --instanceGroup cbgateway --nodecount 1 --templateName gcptemplate
instancegroup configure --instanceGroup host_group_master_1 --nodecount 1 --templateName gcptemplate
instancegroup configure --instanceGroup host_group_master_2 --nodecount 1 --templateName gcptemplate
instancegroup configure --instanceGroup host_group_master_3 --nodecount 1 --templateName gcptemplate
instancegroup configure --instanceGroup host_group_client_1  --nodecount 1 --templateName gcptemplate
instancegroup configure --instanceGroup host_group_slave_1 --nodecount 3 --templateName gcptemplate
network select --name default-gcp-network
securitygroup select --name all-services-port
stack create --name my-first-stack --region US_CENTRAL1_A
cluster create --description "My first cluster"
```

## Next steps

Congrats! Your cluster should now be up and running. To learn more about it we have some [interesting insights](insights.md) about Cloudbreak clusters.
