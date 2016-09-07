-- // CLOUD-65294 Sync database with domains
-- Migration SQL that makes the change goes here.

-- account_preferences

ALTER TABLE account_preferences
    ALTER COLUMN maxnumberofclusters DROP NOT NULL,
    ALTER COLUMN maxnumberofnodespercluster DROP NOT NULL,
    ALTER COLUMN maxnumberofclustersperuser DROP NOT NULL;

-- blueprint

DELETE FROM blueprint WHERE account IS NULL OR owner IS NULL;
DELETE FROM blueprint WHERE
    blueprintname IS NULL OR
    blueprinttext IS NULL;
UPDATE blueprint SET hostgroupcount = 0 WHERE hostgroupcount IS NULL;

ALTER TABLE blueprint
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN blueprintname SET NOT NULL,
    ALTER COLUMN blueprinttext SET NOT NULL,
    ALTER COLUMN hostgroupcount DROP NOT NULL,
    ALTER COLUMN owner SET NOT NULL;

-- cloudbreakevent

DELETE FROM cloudbreakevent WHERE account IS NULL OR owner IS NULL;
DELETE FROM cloudbreakevent WHERE
    cloud IS NULL OR
    eventmessage IS NULL OR
    eventtimestamp IS NULL OR
    eventtype IS NULL OR
    nodecount IS NULL OR
    region IS NULL OR
    stackid IS NULL OR
    stackname IS NULL OR
    stackstatus IS NULL;

ALTER TABLE cloudbreakevent
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN cloud SET NOT NULL,
    ALTER COLUMN eventmessage SET NOT NULL,
    ALTER COLUMN eventtimestamp SET NOT NULL,
    ALTER COLUMN eventtype SET NOT NULL,
    ALTER COLUMN nodecount SET NOT NULL,
    ALTER COLUMN owner SET NOT NULL,
    ALTER COLUMN region SET NOT NULL,
    ALTER COLUMN stackid SET NOT NULL,
    ALTER COLUMN stackname SET NOT NULL,
    ALTER COLUMN stackstatus SET NOT NULL;

-- cloudbreakusage

DELETE FROM cloudbreakusage WHERE account IS NULL OR owner IS NULL;
DELETE FROM cloudbreakusage WHERE
    day IS NULL OR
    instancegroup IS NULL OR
    instancetype IS NULL OR
    provider IS NULL OR
    region IS NULL OR
    stackid IS NULL OR
    stackname IS NULL;
UPDATE cloudbreakusage SET costs = 0.0 WHERE costs IS NULL;
UPDATE cloudbreakusage SET instancehours = 0 WHERE instancehours IS NULL;

ALTER TABLE cloudbreakusage
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN costs SET NOT NULL,
    ALTER COLUMN day SET NOT NULL,
    ALTER COLUMN instancegroup SET NOT NULL,
    ALTER COLUMN instancehours SET NOT NULL,
    ALTER COLUMN instancetype SET NOT NULL,
    ALTER COLUMN owner SET NOT NULL,
    ALTER COLUMN provider SET NOT NULL,
    ALTER COLUMN region SET NOT NULL,
    ALTER COLUMN stackid SET NOT NULL,
    ALTER COLUMN stackname SET NOT NULL;

-- cluster

DELETE FROM cluster WHERE account IS NULL OR owner IS NULL;
UPDATE cluster SET emailneeded = FALSE WHERE emailneeded IS NULL;
UPDATE cluster SET secure = FALSE WHERE secure IS NULL;
UPDATE cluster SET status = 'WAIT_FOR_SYNC' WHERE status IS NULL;
UPDATE cluster SET username = '' WHERE username IS NULL;
UPDATE cluster SET password = '' WHERE password IS NULL;
UPDATE cluster SET configstrategy = 'ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES' WHERE configstrategy IS NULL;

ALTER TABLE cluster
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN emailneeded SET NOT NULL,
    ALTER COLUMN owner SET NOT NULL,
    ALTER COLUMN secure SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN username SET NOT NULL,
    ALTER COLUMN password SET NOT NULL,
    ALTER COLUMN configstrategy SET NOT NULL;

-- clustertemplate

DELETE FROM clustertemplate WHERE account IS NULL OR owner IS NULL;

ALTER TABLE clustertemplate
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN owner SET NOT NULL;

-- component

ALTER TABLE component
    ALTER COLUMN componenttype DROP NOT NULL,
    ALTER COLUMN attributes DROP NOT NULL;

-- constrainttemplate

DELETE FROM constrainttemplate WHERE account IS NULL OR owner IS NULL;
UPDATE constrainttemplate SET status = 'USER_MANAGED' WHERE status IS NULL;

ALTER TABLE constrainttemplate
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN owner SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;

-- credential

DELETE FROM credential WHERE account IS NULL OR owner IS NULL;

ALTER TABLE credential
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN owner SET NOT NULL;

-- flowlog

DELETE FROM flowlog WHERE stackid IS NULL;
UPDATE flowlog SET finalized = TRUE WHERE finalized IS NULL;

ALTER TABLE flowlog
    ALTER COLUMN stackid SET NOT NULL,
    ALTER COLUMN finalized SET NOT NULL;

-- hostgroup

DELETE FROM hostgroup WHERE cluster_id IS NULL;
UPDATE hostgroup SET name = 'undefined' WHERE name IS NULL;

ALTER TABLE hostgroup
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN cluster_id SET NOT NULL;

-- hostmetadata

DELETE FROM hostmetadata WHERE hostgroup_id IS NULL;
UPDATE hostmetadata SET hostname = 'undefined' WHERE hostname IS NULL;
UPDATE hostmetadata SET hostmetadatastate = 'UNHEALTHY' WHERE hostmetadatastate IS NULL;

ALTER TABLE hostmetadata
    ALTER COLUMN hostname SET NOT NULL,
    ALTER COLUMN hostgroup_id SET NOT NULL,
    ALTER COLUMN hostmetadatastate SET NOT NULL;

-- instancegroup

DELETE FROM instancegroup WHERE stack_id IS NULL;
UPDATE instancegroup SET groupname = 'undefined' WHERE groupname IS NULL;
UPDATE instancegroup SET instancegrouptype = 'CORE' WHERE instancegrouptype IS NULL;
UPDATE instancegroup SET nodecount = 0 WHERE nodecount IS NULL;

ALTER TABLE instancegroup
    ALTER COLUMN groupname SET NOT NULL,
    ALTER COLUMN instancegrouptype SET NOT NULL,
    ALTER COLUMN nodecount SET NOT NULL,
    ALTER COLUMN stack_id SET NOT NULL;

-- instancemetadata

UPDATE instancemetadata SET instancestatus = 'FAILED' WHERE instancestatus IS NULL;

ALTER TABLE instancemetadata
    ALTER COLUMN instancestatus SET NOT NULL;

-- ldapconfig

DELETE FROM ldapconfig WHERE account IS NULL OR owner IS NULL;
UPDATE ldapconfig SET serverssl = FALSE WHERE serverssl IS NULL;
UPDATE ldapconfig SET binddn = 'undefined' WHERE binddn IS NULL;
UPDATE ldapconfig SET bindpassword = 'undefined' WHERE bindpassword IS NULL;

ALTER TABLE ldapconfig
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN owner SET NOT NULL,
    ALTER COLUMN serverssl SET NOT NULL,
    ALTER COLUMN binddn SET NOT NULL,
    ALTER COLUMN bindpassword SET NOT NULL;

-- network

DELETE FROM network WHERE account IS NULL OR owner IS NULL;
UPDATE network SET status = 'USER_MANAGED' WHERE status IS NULL;

ALTER TABLE network
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN owner SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;

-- rdsconfig

DELETE FROM rdsconfig WHERE databasetype IS NULL;
UPDATE rdsconfig SET connectionurl = 'undefined' WHERE connectionurl IS NULL;
UPDATE rdsconfig SET connectionusername = 'undefined' WHERE connectionusername IS NULL;
UPDATE rdsconfig SET connectionpassword = 'undefined' WHERE connectionpassword IS NULL;

ALTER TABLE rdsconfig
    ALTER COLUMN connectionurl SET NOT NULL,
    ALTER COLUMN connectionusername SET NOT NULL,
    ALTER COLUMN connectionpassword SET NOT NULL,
    ALTER COLUMN databasetype SET NOT NULL,
    ALTER COLUMN creationdate DROP NOT NULL;

-- recipe

DELETE FROM recipe WHERE account IS NULL OR owner IS NULL;
UPDATE recipe SET name = 'undefined' WHERE name IS NULL;

ALTER TABLE recipe
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN owner SET NOT NULL;

-- resource

DELETE FROM resource WHERE resourcetype IS NULL OR resourcestatus IS NULL;

ALTER TABLE resource
    ALTER COLUMN resourcetype SET NOT NULL,
    ALTER COLUMN resourcestatus SET NOT NULL;

-- securitygroup

DELETE FROM securitygroup WHERE account IS NULL OR owner IS NULL;
UPDATE securitygroup SET status = 'USER_MANAGED' WHERE status IS NULL;

ALTER TABLE securitygroup
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN owner SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;

-- sssdconfig

DELETE FROM sssdconfig WHERE account IS NULL OR owner IS NULL;

ALTER TABLE sssdconfig
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN owner SET NOT NULL;

-- stack

DELETE FROM stack WHERE account IS NULL OR owner IS NULL;
UPDATE stack SET onfailureactionaction = 'ROLLBACK' WHERE onfailureactionaction IS NULL;
UPDATE stack SET region = 'undefined' WHERE region IS NULL;
UPDATE stack SET status = 'WAIT_FOR_SYNC' WHERE status IS NULL;

ALTER TABLE stack
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN onfailureactionaction SET NOT NULL,
    ALTER COLUMN owner SET NOT NULL,
    ALTER COLUMN region SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN created DROP NOT NULL,
    ALTER COLUMN gatewayport DROP NOT NULL;

-- template

DELETE FROM template WHERE account IS NULL OR owner IS NULL;
UPDATE template SET volumecount = 1 WHERE volumecount IS NULL;
UPDATE template SET instancetype = 'undefined' WHERE instancetype IS NULL;
UPDATE template SET status = 'USER_MANAGED' WHERE status IS NULL;

ALTER TABLE template
    ALTER COLUMN account SET NOT NULL,
    ALTER COLUMN owner SET NOT NULL,
    ALTER COLUMN volumecount SET NOT NULL,
    ALTER COLUMN instancetype SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;

-- topology

DELETE FROM topology WHERE owner IS NULL;
UPDATE topology SET deleted = FALSE WHERE deleted IS NULL;

ALTER TABLE topology
    ALTER COLUMN owner SET NOT NULL,
    ALTER COLUMN deleted SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

