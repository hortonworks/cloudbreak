##Supported components

Ambari supports the concept of stacks and associated services in a stack definition. By leveraging the stack definition, Ambari has a consistent and defined interface to install, manage and monitor a set of services and provides extensibility model for new stacks and services to be introduced.

At high level the supported list of components can be grouped in to main categories: Master and Slave - and bundling them together form a Hadoop Service.

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
| NAGIOS		  | NAGIOS_SERVER                                                           | 
| OOZIE		    | OOZIE_CLIENT, OOZIE_SERVER                                              | 
| PIG		      | PIG                                                                     | 
| SQOOP		    | SQOOP                                                                   | 
| STORM		    | DRPC_SERVER, NIMBUS, STORM_REST_API, STORM_UI_SERVER, SUPERVISOR        | 
| TEZ		      | TEZ_CLIENT                                                              | 
| FALCON		  | FALCON_CLIENT, FALCON_SERVER                                            | 
| ZOOKEEPER	  | ZOOKEEPER_CLIENT, ZOOKEEPER_SERVER                                      | 


We provide a list of default Hadoop cluster Blueprints for your convenience, however you can always build and use your own Blueprint.

1. Simple single node - Apache Ambari blueprint

This is a simple [Blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/single-node-hdfs-yarn) which allows you to launch a single node, pseudo-distributed Hadoop Cluster in the cloud. 

It's allows you to use the following services: HDFS, YARN, MAPREDUCE2.

2. Full stack single node - HDP 2.1 blueprint

This is a complex [Blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/hdp-singlenode-default) which allows you to launch a single node, pseudo-distributed Hadoop Cluster in the cloud. 

It's allows you to use the following services: HDFS, YARN, MAPREDUCE2, GANGLIA, HBASE, HIVE, HCATALOG, WEBHCAT, NAGIOS, OOZIE, PIG, SQOOP, STORM, TEZ, FALCON, ZOOKEEPER.

3. Simple multi node - Apache Ambari blueprint 

This is a simple [Blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/multi-node-hdfs-yarn) which allows you to launch a multi node, fully distributed Hadoop Cluster in the cloud. 

It's allows you to use the following services: HDFS, YARN, MAPREDUCE2.

4. Full stack multi node - HDP 2.1 blueprint

This is a complex [Blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/hdp-multinode-default) which allows you to launch a multi node, fully distributed Hadoop Cluster in the cloud. 

It's allows you to use the following services: HDFS, YARN, MAPREDUCE2, GANGLIA, HBASE, HIVE, HCATALOG, WEBHCAT, NAGIOS, OOZIE, PIG, SQOOP, STORM, TEZ, FALCON, ZOOKEEPER.

5. Custom blueprints

We allow you to build your own Blueprint - for further instractions please check the Apache Ambari [documentation](https://cwiki.apache.org/confluence/display/AMBARI/Blueprints).

When you are creating clustom Blueprints you can use the components above to build Hadoop services and use them in your on-demand Hadoop cluster.

We are trying to figure out the hosts to hostgroups assignements - and order to do so you will need to follow the conventions below:

_Note: Apache Ambari community and SequenceIQ is working on an auto-hostgroup assignement algorithm; in the meantime please follow our conventions and check the default blueprints as examples, or ask us to support you._

1. When you are creating a Single node blueprint the name of the default host group has to be `master`.
2. When yoy are creating a Multi node blueprint, all the worker node components (a.k.a. Slaves) will have to be grouped in host groups named `slave_*`. _Replace * with the number of Slave hostgroups_.

The default rules are that for multi node clusters are there must be at least as many hosts as the number of host groups. Each NOT slave host groups (master, gateway, etc) will be launched wiht a cardinality of 1 (1 node per master, gateway, etc hosts), and all the rest of the nodes are equally distributed among Slave nodes (if there are multiple slave host groups).
