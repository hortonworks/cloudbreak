
Once Cloudbreak is up and running you can launch clusters in two different ways. You can use the [Cloudbreak UI](openstack_cb_ui.md) or use the [Cloudbreak shell](openstack_cb_shell.md).

## Manage cloud credentials

You can now log into the Cloudbreak application at http://PUBLIC_IP:3000. Once logged in go to **Manage credentials**. Using manage credentials will  link your cloud account with the Cloudbreak account.

`Name:` name of your credential

`Description:` short description of your linked credential

`User:` your OpenStack user

`Password:` your password

`Tenant Name:` OpenStack tenant name

`Endpoint:` Openstack Identity Service (Keystone) endpont (e.g. http://PUBLIC_IP:5000/v2.0)

`SSH certificate:` the SSH public certificate in OpenSSH format that's private keypair can be used to log into the launched instances later with the ssh username **centos**

`Public in account:` share it with others in the account


##Manage resources

Using manage resources you can create infrastructure templates. Templates describes the infrastructure where the HDP cluster will be provisioned. We support heterogenous clusters - this means that one cluster can be built by combining different templates.

`Name:` name of your template

`Description:` short description of your template

`Instance type:` the OpenStack instance type to be used

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

`Subnet (CIDR):` a subnet with CIDR block under the given `public network`

`Public network ID` id of an OpenStack public network

`Public in account:` share it with others in the account

## Manage security groups

Security groups allows configuration of traffic/access to the cluster. Currently there are two default groups, and later versions will allow setup of new groups.

`only-ssh-and-ssl:` all ports are locked down (you can't access Hadoop services outside of the VPN) but 

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


## Create a cluster

Using the create cluster functionality Cloudbreak will create a cloud Stack and a Hadoop Cluster. In order to create a cluster you will have to select a credential first.

`Cluster name:` your cluster name

`Region:` the region where the cluster is started

`Network:` the network template

`Security Group:` the security group

`Blueprint:` your Hadoop cluster blueprint. Once the blueprint is selected we parse it and give you the option to select the followings for each **hostgroup**.

`Hostgroup configuration`

*  `Group size:` the number of instances to be started
*  `Template:` the stack template associated to the hostgroup

`Public in account:` share it with others in the account

`Enable security:` Install KDC and Kerberize the cluster

**Advanced features**:

`Consul server count:` the number of Consul servers (odd number), by default is 3. It varies with the cluster size.

`Platform variant:` Cloudbreak provides two implementation for creating OpenStack cluster

* `HEAT:` using heat template to create the resources
* `NATIVE:` using API calls to create the resources

`Minimum cluster size:` the provisioning strategy in case of the cloud provider can't allocate all the requested nodes

`Validate blueprint:` feature to validate or not the Ambari blueprint. By default is switched on.

`Ambari Repository config:` you can take the stack RPM's from a custom stack repository

Once you have launched the cluster creation you can track the progress either on Cloudbreak UI or your cloud provider management UI.
