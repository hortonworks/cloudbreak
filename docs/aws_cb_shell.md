# Interactive mode

Start the shell with `cbd util cloudbreak-shell`. This will launch the Cloudbreak shell inside a Docker container and you are ready to start using it.
You have to copy files into the cbd working directory, which you would like to use from shell.

### Create a cloud credential

In order to start using Cloudbreak you will need to have an AWS cloud use configured. Note that Cloudbreak **does not** store you cloud user details - we work around the concept of [IAM](http://aws.amazon.com/iam/) - on Amazon (or other cloud providers) you will have to create an IAM role, a policy and associate that with your Cloudbreak account.

```
credential create --EC2 --description "description" --name my-aws-credential --roleArn <arn role> --sshKeyUrl <URL towards your AWS public key>
```

Alternatively you can upload your public key from a file as well, by using the `—sshKeyPath` switch. You can check whether the credential was creates successfully by using the `credential list` command. You can switch between your cloud credential - when you’d like to use one and act with that you will have to use:

```
credential select --name my-aws-credential
```

### Create a template

A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. A template can be used repeatedly to create identical copies of the same stack (or to use as a foundation to start a new stack).

```
template create --EC2 --name awstemplate --description aws-template --instanceType M3Xlarge --volumeSize 100 --volumeCount 2
```
You can check whether the template was created successfully by using the `template list` or `template show` command.

### Create or select a blueprint

You can define Hadoop cluster blueprints with cloudbreak-shell:

```
blueprint add --name myblueprint --description myblueprint-description --url <url of blueprint>
```

Other available options:

- --file
- --publicInAccount

We ship default Hadoop cluster blueprints with Cloudbreak. You can use these blueprints or add yours. To see the available blueprints and use one of them please use:

```
blueprint list

blueprint select --name hdp-small-default
```

### Create a network

A network gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related networking, maintaining and updating them in an orderly and predictable fashion. A network can be used repeatedly to create identical copies of the same stack (or to use as a foundation to start a new stack).

```
network create --EC2 --name awsnetwork --description aws-network --subnet 10.0.0.0/16
```

Other available options:

- --vpcID "string": your existing vpc on amazon

- --internetGatewayID "string": your amazon internet gateway

- --publicInAccount "flag": flags if the network is public in the account

There is a default network with name "default-aws-network".

You can check whether the network was created successfully by using the `network list` command. Check the network and select it if you are happy with it:

```
network show --name awsnetwork

network select --name awsnetwork
```

### Create a security group

A security group gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related security rules, maintaining and updating them in an orderly and predictable fashion. A security group can be used repeatedly to create identical copies of the same stack (or to use as a foundation to start a new stack).

```
securitygroup create --name secgroup_example --description securitygroup-example --rules 0.0.0.0/0:tcp:8080,9090;10.0.33.0/24:tcp:1234,1235
```

You can check whether the security group was created successfully by using the `securitygroup list` command. Check the security group and select it if you are happy with it:

```
securitygroup show --name secgroup_example

securitygroup select --name secgroup_example
```

There are two default security groups defined: "all-services-port" and "only-ssh-and-ssl"

## Configure instance groups

You have to configure the instancegroups before the provisioning. An instancegroup is defining a group of your nodes with a specified template. Usually we create instancegroups for the hostgroups defined in the blueprints.

```
instancegroup configure --instanceGroup host_group_slave_1 --nodecount 3 --templateName minviable-aws
```

Other available options:

- --templateId "string": Id of the template

### Create a Hadoop cluster
You are almost done - two more command and this will create your Hadoop cluster on your favorite cloud provider. Same as the API, or UI this will use your `template`, and by using CloudFormation will launch a cloud stack
```
stack create --name my-first-stack
```
Once the `stack` is up and running (cloud provisioning is done) it will use your selected `blueprint` and install your custom Hadoop cluster with the selected components and services.
```
cluster create --description "my first cluster"
```
You are done - you can check the progress through the Ambari UI. If you log back to Cloudbreak UI you can check the progress over there as well, and learn the IP address of Ambari.

# Silent mode

With Cloudbreak shell you can recreate clusters based on earlier deployments. Each time you start the shell the executed commands are logged in a file line by line and later either with the `script` command or specifying an `—cmdfile` option the same commands can be executed again.

With `cbd util cloudbreak-shell-quiet` you can specify a shell file and let the shell apply the configs step by step in a silent mode.

# Example

The following example creates a hadoop cluster with `hdp-small-default` blueprint on M3Xlarge instances with 2X100G attached disks on `default-aws-network` network using `all-services-port` security group:

```
credential create --EC2 --description description --name my-aws-credential --roleArn <arn role> --sshKeyPath <path of your AWS public key>
credential select --name mvCredentialName
template create --EC2 --name awstemplate --description aws-template --instanceType M3Xlarge --volumeSize 100 --volumeCount 2
blueprint select --name hdp-small-default
instancegroup configure --instanceGroup cbgateway --nodecount 1 --templateName minviable-aws
instancegroup configure --instanceGroup host_group_master_1 --nodecount 1 --templateName awstemplate
instancegroup configure --instanceGroup host_group_master_2 --nodecount 1 --templateName awstemplate
instancegroup configure --instanceGroup host_group_master_3 --nodecount 1 --templateName awstemplate
instancegroup configure --instanceGroup host_group_client_1  --nodecount 1 --templateName awstemplate
instancegroup configure --instanceGroup host_group_slave_1 --nodecount 3 --templateName awstemplate
network select --name default-aws-network
securitygroup select --name all-services-port
stack create --name my-first-stack --region US_EAST_1
cluster create --description "My first cluster"
```
