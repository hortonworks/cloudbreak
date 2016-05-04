-- // CLOUD-57201 change default gateway port to 9443
-- Migration SQL that makes the change goes here.

UPDATE securitygroup SET description = CONCAT('Open ports: 9443 (Gateway) 22 (SSH) ',
'2022 (SSH) 443 (HTTPS) 8080 (Ambari) 8500 (Consul) 50070 (NameNode) ',
'8088 (RM Web) 8030 (RM Scheduler) 8050 (RM IPC) 19888 (Job History Server) ',
'60010 (HBase Master Web) 60000 (HBase Master) 16020 (HBase Region Server) ',
'60030 (HBase Region Server Info) 9083 (Hive Metastore) 10000 (Hive Server) ',
'10001 (Hive Server Http) 9999 (Accumulo Master) 9997 (Accumulo Tserver) ',
'21000 (Atlas) 8443 (Knox GW) 15000 (Falcon) 8744 (Storm) 11000 (Oozie) ',
'18080 (Spark History server) 8042 (Container logs) 9996 (Zeppelin web socket) ',
'9995 (Zeppelin UI) 6080 (Ranger Admin UI) 3080 (Kibana) 9200 (Elastic Search) ',
'3376 (Swarm) 7070 (Shipyard)')
WHERE name = 'all-services-port';

UPDATE securityrule SET ports = CONCAT('9443,22,2022,443,8080,8500,50070,8088,8030,8050,',
'19888,60010,60000,16020,60030,9083,10000,10001,9999,9997,21000,8443,15000,8744,11000,',
'18080,8042,9996,9995,6080,3080,9200,3376,7070')
WHERE securitygroup_id = (SELECT id FROM securitygroup WHERE name = 'all-services-port');

UPDATE securitygroup
SET description = 'Open ports: 22 (SSH) 2022 (SSH) 9443 (Gateway)'
WHERE name = 'only-ssh-and-ssl';

UPDATE securityrule SET ports = '22,2022,9443'
WHERE securitygroup_id = (SELECT id FROM securitygroup WHERE name = 'only-ssh-and-ssl');

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE securitygroup SET description = CONCAT('Open ports: 22 (SSH) ',
'2022 (SSH) 443 (HTTPS) 8080 (Ambari) 8500 (Consul) 50070 (NameNode) ',
'8088 (RM Web) 8030 (RM Scheduler) 8050 (RM IPC) 19888 (Job History Server) ',
'60010 (HBase Master Web) 60000 (HBase Master) 16020 (HBase Region Server) ',
'60030 (HBase Region Server Info) 9083 (Hive Metastore) 10000 (Hive Server) ',
'10001 (Hive Server Http) 9999 (Accumulo Master) 9997 (Accumulo Tserver) ',
'21000 (Atlas) 8443 (Knox GW) 15000 (Falcon) 8744 (Storm) 11000 (Oozie) ',
'18080 (Spark History server) 8042 (Container logs) 9996 (Zeppelin web socket) ',
'9995 (Zeppelin UI) 6080 (Ranger Admin UI) 3080 (Kibana) 9200 (Elastic Search) ',
'3376 (Swarm) 7070 (Shipyard)')
WHERE name = 'all-services-port';

UPDATE securityrule SET ports = CONCAT('22,2022,443,8080,8500,50070,8088,8030,8050,'
'19888,60010,60000,16020,60030,9083,10000,10001,9999,9997,21000,8443,15000,8744,11000,'
'18080,8042,9996,9995,6080,3080,9200,3376,7070')
WHERE securitygroup_id = (SELECT id FROM securitygroup WHERE name = 'all-services-port');

UPDATE securitygroup
SET description = 'Open ports: 22 (SSH) 2022 (SSH) 443 (HTTPS)'
WHERE name = 'only-ssh-and-ssl';

UPDATE securityrule SET ports = '22,2022,443'
WHERE securitygroup_id = (SELECT id FROM securitygroup WHERE name = 'only-ssh-and-ssl');