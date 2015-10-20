# Interactive mode

Start the shell with `cbd util cloudbreak-shell`. This will launch the Cloudbr shell inside a Docker container and you are ready to start using it.

### Create a cloud credential

```
credential create --AZURE --description "description" --name "myCredentialName" --subscriptionId "mySubscriptionId" --tenantId "sdfsd-sdfdsf-dsfdsf-sdfsdf" --secretKey "sdfdsfs-sdfsddsf-sdfsdf-sdfsdf" --accesKey "acceskey" --sshKeyUrl "URL towards your public SSH key file"
```

Alternatively you can upload your public key from a file as well, by using the `—sshKeyPath` switch. You can check whether the credential was creates successfully by using the `credential list` command.
You can switch between your cloud credential - when you’d like to use one and act with that you will have to use:
```
credential select --id #ID of the credential
or
credential select --name #NAME of the credential
```

You can delete your cloud credential - when you’d like to delete one you will have to use:
```
credential delete --id #ID of the credential
or
credential delete --name #NAME of the credential
```

You can show your cloud credential - when you’d like to show one you will have to use:
```
credential show --id #ID of the credential
or
credential show --name #NAME of the credential
```

### Create a template

A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion. A template can be used repeatedly to create identical copies of the same stack (or to use as a foundation to start a new stack).

```
template create --AZURE --name azuretemplate --description azure-template --instanceType STANDARD_D2 --volumeSize 100 --volumeCount 2
```
You can check whether the template was created successfully by using the `template list` command.
Check the template and select it if you are happy with it:
```
template select --id #ID of the template
or
template select --name #NAME of the template
```

You can delete your cloud template - when you’d like to delete one you will have to use:
```
template delete --id #ID of the template
or
template delete --name #NAME of the template
```

You can show your cloud template - when you’d like to show one you will have to use:
```
template show --id #ID of the template
or
template show --name #NAME of the template
```

### Create a stack

Stacks are template `instances` - a running cloud infrastructure created based on a template. Use the following command to create a stack to be used with your Hadoop cluster:

```
stack create --name "myStackName" --nodeCount 10
```
### Select a blueprint

We ship default Hadoop cluster blueprints with Cloudbreak. You can use these blueprints or add yours. To see the available blueprints and use one of them please use:

```
blueprint list

blueprint select --id #ID of the blueprint
```
### Create a Hadoop cluster
You are almost done - one more command and this will create your Hadoop cluster on your favorite cloud provider. Same as the API, or UI this will use your `template`, and by using CloudFormation will launch a cloud `stack` - once the `stack` is up and running (cloud provisioning is done) it will use your selected `blueprint` and install your custom Hadoop cluster with the selected components and services.

```
cluster create --description "my cluster desc"
```
You are done - you can check the progress through the Ambari UI. If you log back to Cloudbreak UI you can check the progress over there as well, and learn the IP address of Ambari.

# Silent mode

With Cloudbreak shell you can recreate clusters based on earlier deployments. Each time you start the shell the executed commands are logged in a file line by line and later either with the `script` command or specifying an `—cmdfile` option the same commands can be executed again.

With c`bd util cloudbreak-shell-quiet` you can specify a shell file and let the shell apply the configs step by step in a silent mode.
