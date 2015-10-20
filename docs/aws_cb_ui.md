You can now log into the Cloudbreak application at http://PUBLIC_IP:3000.

## Manage cloud credentials

Using manage credentials will  link your cloud account with the Cloudbreak account.

`Name:` name of your credential

`Description:` short description of your linked credential

`Role ARN:` the role string - you can find it at the summary tab of the IAM role, default is *cbreak -deployer*

`SSH public key:` an SSH public key in OpenSSH format that's private keypair can be used to log into the launched instances later

`Public in account:` share it with others in the account

The ssh username is **ec2-user**

##Manage resources

Using manage resources you can create infrastructure templates. Templates describes the infrastructure where the HDP cluster will be provisioned. We support heterogenous clusters - this means that one cluster can be built by combining different templates.

`Name:` name of your template

`Description:` short description of your template

`Instance type:` the Amazon instance type to be used - we suggest to use at least small or medium instances

`Volume type:` option to choose are SSD, regular HDD (both EBS) or Ephemeral

`Attached volumes per instance:` the number of disks to be attached

`Volume size (GB):` the size of the attached disks (in GB)

`Spot price:` option to set a spot price - not mandatory, if specified we will request spot price instances (which might take a while or never be fulfilled by Amazon)

`EBS encryption:` this feature is supported with all EBS volume types (General Purpose (SSD), Provisioned IOPS (SSD), and Magnetic

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

`Public in account:` share it with others in the account

## Manage security groups
Security groups allows configuration of traffic/access to the cluster. Currently there are two default groups, and later versions will allow setup of new groups.

`only-ssh-and-ssl:` all ports are locked down (you can't access Hadoop services outside of the VPN) but SSH (22) and HTTPS (443)

`all-services-port:` all Hadoop services + SSH/HTTP are accessible by default: SSH (22) HTTPS (443) 8080 (Ambari) 8500 (Consul) 50070 (NN) 8088 (RM Web) 8030 (RM Scheduler) 8050 (RM IPC) 19888 (Job history server) 60010 (HBase master) 15000 (Falcon) 8744 (Storm) 11000 (Oozie) 18080 (Spark HS) 8042 (NM Web) 9996 (Zeppelin WebSocket) 9995 (Zeppelin UI) 3080 (Kibana) 9200 (Elasticsearch)

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

`Enable security:` Install KDC and Kerberize the cluster

`Public in account:` share it with others in the account

**Advanced features**:

`Consul server count:` the number of Consul servers (odd number), by default is 3. It varies with the cluster size.

`Minimum cluster size:` the provisioning strategy in case of the cloud provider can't allocate all the requested nodes

`Validate blueprint:` feature to validate or not the Ambari blueprint. By default is switched on.

`Dedicated instances:` AWS allows to use dedicated instances

`Ambari Repository config:` you can take the stack RPM's from a custom stack repository

Once you have launched the cluster creation you can track the progress either on Cloudbreak UI or your cloud provider management UI.
