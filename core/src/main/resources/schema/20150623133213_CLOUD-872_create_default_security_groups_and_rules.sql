-- // CLOUD-872 create default security groups and rules
-- Migration SQL that makes the change goes here.

-- create temporary table for storing accounts that does not have default securitygroup and
-- the created id of the default security groups for them
CREATE TABLE create_security_groups_temp(
  account         CHARACTER VARYING (255),
  owner           CHARACTER VARYING (255),
  securitygroup_id  bigint
);


WITH insert_temp AS (
  INSERT INTO securitygroup (
      name,
      description,
      account,
      owner,
      publicinaccount,
      status
  )
    SELECT
      'all-services-port' AS name,
      'Open ports: 8080 (Ambari) 8500 (Consul) 50070 (NN) 8088 (RM Web) 8030(RM Scheduler) 8050(RM IPC) 19888(Job history server) 60010(HBase master) 15000(Falcon) 8744(Storm) 11000(Oozie) 18080(Spark HS) 8042(NM Web) 9996(Zeppelin WebSocket) 9995(Zeppelin UI) 3080(Kibana) 9200(Elasticsearch)' AS description,
      csgt.account AS account,
      csgt.owner AS owner,
      true AS publicinaccount,
      'DEFAULT' AS status
    FROM (  SELECT
              s.account AS account,
              max(s.owner) AS owner
            FROM stack s
              where s.account NOT IN (select sg.account from securitygroup sg) AND s.status <> 'DELETE_COMPLETED'
              group by s.account) csgt
  RETURNING id AS securitygroup_id, account AS account, owner AS owner
)
INSERT INTO create_security_groups_temp(
    securitygroup_id,
    account,
    owner
  ) SELECT securitygroup_id,account,owner from insert_temp;


-- insert default security rules for the newly created security groups
INSERT INTO securityrule (
  securitygroup_id,
  cidr,
  ports,
  protocol,
  modifiable
)
SELECT
  s.id AS securitygroup_id,
  '0.0.0.0/0' AS cidr,
  '22,443,8080,8500,50070,8088,8030,8050,19888,60010,15000,8744,11000,18080,8042,9996,9995,3080,9200' AS ports,
  'tcp' AS protocol,
  false AS modifiable
FROM securitygroup s
  where s.account IN (SELECT account FROM create_security_groups_temp);


UPDATE stack SET securitygroup_id = subquery.id
  FROM (SELECT id, account FROM securitygroup) AS subquery
  WHERE
      stack.account=subquery.account AND
      stack.securitygroup_id IS NULL AND
      stack.status <> 'DELETE_COMPLETED';

-- drop the temporary table
DROP TABLE IF EXISTS create_security_groups_temp;

-- //@UNDO
-- SQL to undo the change goes here.


