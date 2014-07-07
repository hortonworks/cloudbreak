##Supported components

Ambari supports the concept of stacks and associated services in a stack definition. By leveraging the stack definition, Ambari has a consistent and defined interface to install, manage and monitor a set of services and provides extensibility model for new stacks and services to be introduced.

At high level the supported list of components can be grouped in to main categories: Master and Slave - and bundling them together form a Hadoop Service.

| Services    | Components                                                              |
| ----------- |:------------------------------------------------------------------------| 
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
