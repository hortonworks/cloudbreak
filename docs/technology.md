#Technology

##Cloudbreak deployer architecture

- **uaa**: OAuth Identity Server
- **cloudbreak** - the Cloudbreak app
- **periscope** - the Periscope app
- **uluwatu** - Cloudbreak UI
- **sultans** - user management

###System Level Containers

- consul: Service Registry
- registrator: automatically registers/unregisters containers with Consul

##Cloudbreak application architecture

Cloudbreak is built on the foundation of cloud providers APIs, Apache Ambari, Docker containers, Swarm and Consul.

###Apache Ambari

The Apache Ambari project is aimed at making Hadoop management simpler by developing software for provisioning, managing, and monitoring Apache Hadoop clusters. Ambari provides an intuitive, easy-to-use Hadoop management web UI backed by its RESTful APIs.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/1.1.0/docs/diagrams/ambari-overview.png)

Ambari enables System Administrators to:

1. Provision a Hadoop Cluster
  * Ambari provides a step-by-step wizard for installing Hadoop services across any number of hosts.
  * Ambari handles configuration of Hadoop services for the cluster.

2. Manage a Hadoop Cluster
  * Ambari provides central management for starting, stopping, and reconfiguring Hadoop services across the entire cluster.

3. Monitor a Hadoop Cluster
  * Ambari provides a dashboard for monitoring health and status of the Hadoop cluster.
  * Ambari allows to choose between predefined alerts or add yur custom ones

Ambari enables to integrate Hadoop provisioning, management and monitoring capabilities into applications with the Ambari REST APIs.
Ambari Blueprints are a declarative definition of a cluster. With a Blueprint, you can specify a Stack, the Component layout and the Configurations to materialise a Hadoop cluster instance (via a REST API) without having to use the Ambari Cluster Install Wizard.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/1.1.0/docs/diagrams/ambari-create-cluster.png)

###Docker

Docker is an open platform for developers and sysadmins to build, ship, and run distributed applications. Consisting of Docker Engine, a portable, lightweight runtime and packaging tool, and Docker Hub, a cloud service for sharing applications and automating workflows, Docker enables apps to be quickly assembled from components and eliminates the friction between development, QA, and production environments. As a result, IT can ship faster and run the same app, unchanged, on laptops, data center VMs, and any cloud.

The main features of Docker are:

1. Lightweight, portable
2. Build once, run anywhere
3. VM - without the overhead of a VM
  * Each virtualised application includes not only the application and the necessary binaries and libraries, but also an entire guest operating system
  * The Docker Engine container comprises just the application and its dependencies. It runs as an isolated process in userspace on the host operating system, sharing the kernel with other containers.
    ![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/1.1.0/docs/diagrams/vm.png)

4. Containers are isolated
5. It can be automated and scripted

###Swarm

Docker Swarm is native clustering for Docker. It turns a pool of Docker hosts into a single, virtual host. Swarm serves the standard Docker API.

  * Distributed container orchestration: Allows to remotely orchestrate Docker containers on different hosts
    ![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/1.1.0/docs/diagrams/swarm.png)
  * Discovery services: Supports different discovery backends to provide service discovery, as such: token (hosted) and file based, etcd, Consul, Zookeeper.
  * Advanced scheduling: Swarm will schedule containers on hosts based on different filters and strategies

###Consul

Consul it is a tool for discovering and configuring services in your infrastructure. It provides several key features

  * Service Discovery: Clients of Consul can provide a service, such as api or mysql, and other clients can use Consul to discover providers of a given service. Using either DNS or HTTP, applications can easily find the services they depend upon.

  * Health Checking: Consul clients can provide any number of health checks, either associated with a given service ("is the webserver returning 200 OK"), or with the local node ("is memory utilization below 90%"). This information can be used by an operator to monitor cluster health, and it is used by the service discovery components to route traffic away from unhealthy hosts.

  * Key/Value Store: Applications can make use of Consul's hierarchical key/value store for any number of purposes, including dynamic configuration, feature flagging, coordination, leader election, and more. The simple HTTP API makes it easy to use.

  * Multi Datacenter: Consul supports multiple datacenters out of the box. This means users of Consul do not have to worry about building additional layers of abstraction to grow to multiple regions.

    ![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/1.1.0/docs/diagrams/consul.png)

<!--technologies.md-->

<!--components.md-->

##Supported components

Ambari supports the concept of stacks and associated services in a stack definition. By leveraging the stack definition, Ambari has a consistent and defined interface to install, manage and monitor a set of services and provides extensibility model for new stacks and services to be introduced.

At high level the supported list of components can be grouped into main categories: Master and Slave - and bundling them together form a Hadoop Service.

| Services    | Components                                                              |
|:----------- |:------------------------------------------------------------------------|
| HDFS		    | DATANODE, HDFS_CLIENT, JOURNALNODE, NAMENODE, SECONDARY_NAMENODE, ZKFC  |
| YARN		    | APP_TIMELINE_SERVER, NODEMANAGER, RESOURCEMANAGER, YARN_CLIENT          |
| MAPREDUCE2	| HISTORYSERVER, MAPREDUCE2_CLIENT                                        |
| GANGLIA		  | GANGLIA_MONITOR, GANGLIA_SERVER                                         |
| HBASE		    | HBASE_CLIENT, HBASE_MASTER, HBASE_REGIONSERVER                          |
| HIVE		    | HIVE_CLIENT, HIVE_METASTORE, HIVE_SERVER, MYSQL_SERVER                  |
| HCATALOG	  | HCAT                                                                    |
| WEBHCAT		  | WEBHCAT_SERVER                                                          |
| OOZIE		    | OOZIE_CLIENT, OOZIE_SERVER                                              |
| PIG		      | PIG                                                                     |
| SQOOP		    | SQOOP                                                                   |
| STORM		    | DRPC_SERVER, NIMBUS, STORM_REST_API, STORM_UI_SERVER, SUPERVISOR        |
| TEZ		      | TEZ_CLIENT                                                              |
| FALCON		  | FALCON_CLIENT, FALCON_SERVER                                            |
| ZOOKEEPER	  | ZOOKEEPER_CLIENT, ZOOKEEPER_SERVER                                      |
| SPARK	  | SPARK_JOBHISTORYSERVER, SPARK_CLIENT                                      |
| RANGER	  | RANGER_USERSYNC, RANGER_ADMIN                                      |
| AMBARI_METRICS	  | AMBARI_METRICS, METRICS_COLLECTOR, METRICS_MONITOR                                      |
| KERBEROS	  | KERBEROS_CLIENT                                      |
| FLUME	  | FLUME_HANDLER                                      |
| KAFKA	  | KAFKA_BROKER                                      |
| KNOX	  | KNOX_GATEWAY                                      |
| NAGIOS	  | NAGIOS_SERVER                                      |
| ATLAS	  | ATLAS                                     |
| CLOUDBREAK	  | CLOUDBREAK                                      |


We provide a list of default Hadoop cluster Blueprints for your convenience, however you can always build and use your own Blueprint.

* hdp-small-default - HDP 2.3 blueprint

This is a complex [Blueprint](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/core/src/main/resources/defaults/blueprints/hdp-small-default.bp) which allows you to launch a multi node, fully distributed HDP 2.3 Cluster in the cloud.

It allows you to use the following services: HDFS, YARN, MAPREDUCE2, KNOX, HBASE, HIVE, HCATALOG, WEBHCAT, SLIDER, OOZIE, PIG, SQOOP, METRICS, TEZ, FALCON, ZOOKEEPER.

* hdp-streaming-cluster - HDP 2.3 blueprint

This is a streaming [Blueprint](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/core/src/main/resources/defaults/blueprints/hdp-streaming-cluster.bp) which allows you to launch a multi node, fully distributed HDP 2.3 Cluster in the cloud, optimized for streaming jobs.

It allows you to use the following services: HDFS, YARN, MAPREDUCE2, STORM, KNOX, HBASE, HIVE, HCATALOG, WEBHCAT, SLIDER, OOZIE, PIG, SQOOP, METRICS, TEZ, FALCON, ZOOKEEPER.

* hdp-spark-cluster - HDP 2.3 blueprint

This is an analytics [Blueprint](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/core/src/main/resources/defaults/blueprints/hdp-spark-cluster.bp) which allows you to launch a multi node, fully distributed HDP 2.3 Cluster in the cloud, optimized for analytic jobs.

It allows you to use the following services: HDFS, YARN, MAPREDUCE2, SPARK, ZEPPELIN, KNOX, HBASE, HIVE, HCATALOG, WEBHCAT, SLIDER, OOZIE, PIG, SQOOP, METRICS, TEZ, FALCON, ZOOKEEPER.

<!--components.md-->

<!--accounts.md-->
