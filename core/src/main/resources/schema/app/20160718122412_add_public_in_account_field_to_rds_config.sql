-- // add public in account field to rds config
-- Migration SQL that makes the change goes here.

ALTER TABLE rdsconfig
 ADD COLUMN name varchar(255) NOT NULL DEFAULT 'name';

UPDATE rdsconfig SET name=databasetype || '-' || id;

ALTER TABLE rdsconfig
 ADD COLUMN publicinaccount boolean NOT NULL DEFAULT false;

ALTER TABLE rdsconfig
 ADD COLUMN owner varchar(255) NOT NULL DEFAULT '';

UPDATE rdsconfig SET owner=(SELECT owner FROM cluster WHERE cluster.rdsconfig_id=rdsconfig.id)
 WHERE (SELECT owner FROM cluster WHERE cluster.rdsconfig_id=rdsconfig.id) IS NOT NULL;

ALTER TABLE rdsconfig
 ADD COLUMN account varchar(255) NOT NULL DEFAULT '';

UPDATE rdsconfig SET account=(SELECT account FROM cluster WHERE cluster.rdsconfig_id=rdsconfig.id)
 WHERE (SELECT account FROM cluster WHERE cluster.rdsconfig_id=rdsconfig.id) IS NOT NULL;

ALTER TABLE rdsconfig
 ADD COLUMN status varchar(255) NOT NULL DEFAULT 'USER_MANAGED';

ALTER TABLE rdsconfig
 ADD COLUMN creationdate bigint NOT NULL DEFAULT 1469028101000;
UPDATE rdsconfig SET creationdate=(SELECT creationfinished FROM cluster WHERE cluster.rdsconfig_id=rdsconfig.id)
 WHERE (SELECT creationfinished FROM cluster WHERE cluster.rdsconfig_id=rdsconfig.id) IS NOT NULL;

ALTER TABLE ONLY rdsconfig
 ADD CONSTRAINT uk_rdsconfig_account_name UNIQUE (account, name);



-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE rdsconfig
 DROP COLUMN publicinaccount,
 DROP COLUMN name,
 DROP COLUMN owner,
 DROP COLUMN account,
 DROP COLUMN status,
 DROP COLUMN creationdate;

