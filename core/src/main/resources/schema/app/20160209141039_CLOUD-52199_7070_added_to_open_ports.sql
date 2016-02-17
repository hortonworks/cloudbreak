-- // CLOUD-52199 7070 added to open ports
-- Migration SQL that makes the change goes here.


UPDATE securitygroup SET description='Open ports: 8080 (Ambari) 8500 (Consul) 50070 (NN) 8088 (RM Web) 8030(RM Scheduler) 8050(RM IPC) 19888(Job history server) 60000(HBase master) 60010(HBase master web) 16020(HBase RS) 60030(HBase RS info) 15000(Falcon) 8744(Storm) 9083(Hive metastore) 10000(Hive server) 10001(Hive server HTTP) 9999(Accumulo master) 9997(Accumulo Tserver) 21000(Atlas) 8443(KNOX)11000(Oozie) 18080(Spark HS) 8042(NM Web) 9996(Zeppelin WebSocket) 9995(Zeppelin UI) 3080(Kibana) 9200(Elasticsearch) 6080(Ranger) 7070(Shipyard)' WHERE description='Open ports: 8080 (Ambari) 8500 (Consul) 50070 (NN) 8088 (RM Web) 8030(RM Scheduler) 8050(RM IPC) 19888(Job history server) 60000(HBase master) 60010(HBase master web) 16020(HBase RS) 60030(HBase RS info) 15000(Falcon) 8744(Storm) 9083(Hive metastore) 10000(Hive server) 10001(Hive server HTTP) 9999(Accumulo master) 9997(Accumulo Tserver) 21000(Atlas) 8443(KNOX)11000(Oozie) 18080(Spark HS) 8042(NM Web) 9996(Zeppelin WebSocket) 9995(Zeppelin UI) 3080(Kibana) 9200(Elasticsearch) 6080(Ranger)';
UPDATE securityrule SET ports='22,443,8080,8500,50070,8088,8030,8050,19888,60010,60000,16020,60030,9083,10000,10001,9999,9997,21000,8443,15000,8744,11000,18080,8042,9996,9995,3080,9200,3376,6080,7070' WHERE ports='22,443,8080,8500,50070,8088,8030,8050,19888,60010,60000,16020,60030,9083,10000,10001,9999,9997,21000,8443,15000,8744,11000,18080,8042,9996,9995,3080,9200,3376,6080';


-- //@UNDO
-- SQL to undo the change goes here.

UPDATE securitygroup SET description='Open ports: 8080 (Ambari) 8500 (Consul) 50070 (NN) 8088 (RM Web) 8030(RM Scheduler) 8050(RM IPC) 19888(Job history server) 60000(HBase master) 60010(HBase master web) 16020(HBase RS) 60030(HBase RS info) 15000(Falcon) 8744(Storm) 9083(Hive metastore) 10000(Hive server) 10001(Hive server HTTP) 9999(Accumulo master) 9997(Accumulo Tserver) 21000(Atlas) 8443(KNOX)11000(Oozie) 18080(Spark HS) 8042(NM Web) 9996(Zeppelin WebSocket) 9995(Zeppelin UI) 3080(Kibana) 9200(Elasticsearch) 6080(Ranger) 7070(Shipyard)' WHERE description='Open ports: 8080 (Ambari) 8500 (Consul) 50070 (NN) 8088 (RM Web) 8030(RM Scheduler) 8050(RM IPC) 19888(Job history server) 60000(HBase master) 60010(HBase master web) 16020(HBase RS) 60030(HBase RS info) 15000(Falcon) 8744(Storm) 9083(Hive metastore) 10000(Hive server) 10001(Hive server HTTP) 9999(Accumulo master) 9997(Accumulo Tserver) 21000(Atlas) 8443(KNOX)11000(Oozie) 18080(Spark HS) 8042(NM Web) 9996(Zeppelin WebSocket) 9995(Zeppelin UI) 3080(Kibana) 9200(Elasticsearch) 6080(Ranger)';
UPDATE securityrule SET ports='22,443,8080,8500,50070,8088,8030,8050,19888,60010,60000,16020,60030,9083,10000,10001,9999,9997,21000,8443,15000,8744,11000,18080,8042,9996,9995,3080,9200,3376,6080,7070' WHERE ports='22,443,8080,8500,50070,8088,8030,8050,19888,60010,60000,16020,60030,9083,10000,10001,9999,9997,21000,8443,15000,8744,11000,18080,8042,9996,9995,3080,9200,3376,6080';



