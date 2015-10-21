
Once Cloudbreak is up and running you can launch clusters in two different ways. You can use the [Cloudbreak UI](azure_cb_ui.md) or use the [Cloudbreak shell](azure_cb_shell.md).

## Manage cloud credentials

You can now log into the Cloudbreak application at http://PUBLIC_IP:3000. Once logged in go to **Manage credentials**. Using manage credentials will  link your cloud account with the Cloudbreak account.

`Name:` name of your credential

`Description:` short description of your linked credential

`Subscription Id:` your Azure subscription id - see Accounts (Browse all> Subscription)

`Password:` your password which was setted up when you create the AD app

`App ID:` You app Id (Browse all> Subscription> Subscription detail> Users> You application> Properties)

`App Owner Tenant ID:` You Tenant Id (Browse all> Subscription> Subscription detail> Users> You application> Properties)

`SSH public key:` the SSH public key in OpenSSH format that's private keypair can be used to log into the launched instances later

> Cloudbreak supporting simple rsa public key instead of X509 certificate file after 1.0.4 version

The ssh username is **cloudbreak**

##Manage resources

Using manage resources you can create infrastructure templates. Templates describes the infrastructure where the HDP cluster will be provisioned. We support heterogenous clusters - this means that one cluster can be built by combining different templates.

`Name:` name of your template

`Description:` short description of your template

`Instance type:` the Amazon instance type to be used - we suggest to use at least small or medium instances

`Volume type:` option to choose are SSD, regular HDD (both EBS) or Ephemeral

`Attached volumes per instance:` the number of disks to be attached

`Volume size (GB):` the size of the attached disks (in GB)

`Public in account:` share it with others in the account

## Manage blueprints
Blueprints are your declarative definition of a Hadoop cluster.

`Name:` name of your blueprint

`Description:` short description of your blueprint

`Source URL:` you can add a blueprint by pointing to a URL. As an example you can use this [blueprint](https://github.com/sequenceiq/ambari-rest-client/raw/1.6.0/src/main/resources/blueprints/multi-node-hdfs-yarn).

`Manual copy:` you can copy paste your blueprint in this text area

`Public in account:` share it with others in the account

## Manage networks

Manage networks allows you to create or reuse existing networks and configure them.

`Name:` name of the network

`Description:` short description of your network

`Subnet (CIDR):` a subnet in the VPC with CIDR block

`Address prefix (CIDR):` the address space that is used for subnets

`Public in account:` share it with others in the account

## Manage security groups

Security groups allows configuration of traffic/access to the cluster. Currently there are two default groups, and later versions will allow setup of new groups.

`only-ssh-and-ssl:` all ports are locked down (you can't access Hadoop services outside of the VPN)

* SSH (22)
* HTTPS (443)

`all-services-port:` all Hadoop services + SSH/HTTP are accessible by default:

* SSH (22)
* HTTPS (443)
* Ambari (8080)
* Consul (8500)
* NN (50070)
* RM Web (8088)
* RM Scheduler (8030)
* RM IPC (8050)
* Job history server (19888)
* HBase master (60010)
* Falcon (15000)
* Storm (8744)
* Oozie (11000)
* Spark HS (18080)
* NM Web (8042)
* Zeppelin WebSocket (9996)
* Zeppelin UI (9995)
* Kibana (3080)
* Elasticsearch (9200)

## Create a cluster

Using the create cluster functionality Cloudbreak will create a cloud Stack and a Hadoop Cluster. In order to create a cluster you will have to select a credential first.

`Cluster name:` your cluster name

`Region:` the region where the cluster is started

`Network:` the network template

`Security Group:" the security group

`Blueprint:` your Hadoop cluster blueprint. Once the blueprint is selected we parse it and give you the option to select the followings for each **hostgroup**.

`Hostgroup configuration`

  `Group size:` the number of instances to be started

  `Template:` the stack template associated to the hostgroup

`DASH Account Name:` values printed by Cloudbreak deployer when the DASH service was deployed

`DASH Account Key:` values printed by Cloudbreak deployer when the DASH service was deployed

  **Important:** For better performance DASH service and the Cloudbreak cluster must be in the same Azure region.

`Enable security:` Install KDC and Kerberize the cluster

`Public in account:` share it with others in the account

**Advanced features**:

`Consul server count:` the number of Consul servers (odd number), by default is 3. It varies with the cluster size.

`Minimum cluster size:` the provisioning strategy in case of the cloud provider can't allocate all the requested nodes

`Validate blueprint:` feature to validate or not the Ambari blueprint. By default is switched on.

`Ambari Repository config:` you can take the stack RPM's from a custom stack repository

Once you have launched the cluster creation you can track the progress either on Cloudbreak UI or your cloud provider management UI.
