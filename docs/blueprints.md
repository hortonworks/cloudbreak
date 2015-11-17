


# Blueprints

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

## Components

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
