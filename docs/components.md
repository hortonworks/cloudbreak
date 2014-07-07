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

When you are creating clustom Blueprints you can use these components above to build Hadoop services and use them in your on-demand Hadoop cluster.

We provide a list of default Hadoop cluster Blueprints for your convenience. 

1. Simple single node

This is a simple [Blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/single-node-hdfs-yarn) which allows you to launch a single node, pseudo-distributed Hadoop Cluster in the cloud. 

It's allows you to use the following services: HDFS, YARN, MAPREDUCE2.

2. Full stack single node - HDP

This is a complex [Blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/hdp-singlenode-default) which allows you to launch a single node, pseudo-distributed Hadoop Cluster in the cloud. 

It's allows you to use the following services: HDFS, YARN, MAPREDUCE2, GANGLIA, HBASE, HIVE, HCATALOG, WEBHCAT, NAGIOS, OOZIE, PIG, SQOOP, STORM, TEZ, FALCON, ZOOKEEPER.
