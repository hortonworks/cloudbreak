# Interactive mode

Start the shell with `cbd util cloudbreak-shell`. This will launch the Cloudbreak shell inside a Docker container and you are ready to start using it.

You have to copy files into the cbd working directory, which you would like to use from shell. For example if your `cbd` working directory is `~/prj/cbd` then copy your blueprint and public ssh key file into this directory. You can refer to these files with their names from the shell.

## Create a cloud credential

```
credential create --AZURE --description "credential description" --name myazurecredential --subscriptionId <your Azure subscription id> --appId <your Azure application id> --tenantId <your tenant id> --password <your Azure application password> --sshKeyPath <path of your public SSH key file>
```

> Cloudbreak is supporting simple rsa public key instead of X509 certificate file after 1.0.4 version

Alternatively you can upload your public key from an url as well, by using the `—sshKeyUrl` switch. You can check whether the credential was creates successfully by using the `credential list` command.
You can switch between your cloud credential - when you’d like to use one and act with that you will have to use:
```
credential select --name myazurecredential
```

You can delete your cloud credential - when you’d like to delete one you will have to use:
```
credential delete --name myazurecredential
```

You can show your cloud credential - when you’d like to show one you will have to use:
```
credential show --name myazurecredential
```

## Create a template

A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. A template can be used repeatedly to create identical copies of the same stack (or to use as a foundation to start a new stack).

```
template create --AZURE --name azuretemplate --description azure-template --instanceType STANDARD_D3 --volumeSize 100 --volumeCount 2
```
You can check whether the template was created successfully by using the `template list` or `template show` command.

You can delete your cloud template - when you’d like to delete one you will have to use:
```
template delete --name azuretemplate
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
network create --AZURE --name azurenetwork --description azure-network --subnet 10.0.0.0/16 --addressPrefix 10.0.0.0/8
```

Other available options:

`--publicInAccount` flags if the network is public in the account

There is a default network with name `default-azure-network` with 10.0.0.0/16 subnet and 10.0.0.0/8 addressPrefix.

You can check whether the network was created successfully by using the `network list` command. Check the network and select it if you are happy with it:

```
network show --name azurenetwork

network select --name azurenetwork
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
instancegroup configure --instanceGroup host_group_slave_1 --nodecount 3 --templateName minviable-azure
```

Other available options:

`--templateId` Id of the template

## Create a Hadoop cluster
You are almost done - two more command and this will create your Hadoop cluster on your favorite cloud provider. Same as the API, or UI this will use your `credential`, `instancegroups`, `network`, `securitygroup`, and by using Azure ResourceManager will launch a cloud stack
```
stack create --name my-first-stack --region WEST_US
```
Once the `stack` is up and running (cloud provisioning is done) it will use your selected `blueprint` and install your custom Hadoop cluster with the selected components and services.
```
cluster create --description "my first cluster"
```
You are done - you can check the progress through the Ambari UI. If you log back to Cloudbreak UI you can check the progress over there as well, and learn the IP address of Ambari.

# Silent mode

With Cloudbreak shell you can execute script files as well. A script file contains cloudbreak shell commands and can be executed with the `script` cloudbreak shell command

```
script <your script file>
```

or with the `cbd util cloudbreak-shell-quiet` cbd command:

```
cbd util cloudbreak-shell-quiet < example.sh
```

# Example

The following example creates a hadoop cluster with `hdp-small-default` blueprint on STANDARD_D3 instances with 2X100G attached disks on `default-azure-network` network using `all-services-port` security group. You should copy your ssh public key file into your cbd working directory with name `id_rsa.pub` and change the `<...>` parts with your azure credential details.

```
credential create --AZURE --description "credential description" --name myazurecredential --subscriptionId <your Azure subscription id> --appId <your Azure application id> --tenantId <your tenant id> --password <your Azure application password> --sshKeyPath id_rsa.pub
credential select --name myazurecredential
template create --AZURE --name azuretemplate --description azure-template --instanceType STANDARD_D3 --volumeSize 100 --volumeCount 2
blueprint select --name hdp-small-default
instancegroup configure --instanceGroup cbgateway --nodecount 1 --templateName azuretemplate
instancegroup configure --instanceGroup host_group_master_1 --nodecount 1 --templateName azuretemplate
instancegroup configure --instanceGroup host_group_master_2 --nodecount 1 --templateName azuretemplate
instancegroup configure --instanceGroup host_group_master_3 --nodecount 1 --templateName azuretemplate
instancegroup configure --instanceGroup host_group_client_1  --nodecount 1 --templateName azuretemplate
instancegroup configure --instanceGroup host_group_slave_1 --nodecount 3 --templateName azuretemplate
network select --name default-azure-network
securitygroup select --name all-services-port
stack create --name my-first-stack --region WEST_US
cluster create --description "My first cluster"
```

## Next steps

Congrats! Your cluster should now be up and running. To learn more about it we have some [interesting insights](insights.md) about Cloudbreak clusters.
