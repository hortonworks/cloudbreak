-- // CB-3685 salt resiliency
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata ADD COLUMN IF NOT EXISTS statusReason TEXT;

UPDATE instancemetadata SET instancestatus = 'SERVICES_RUNNING' WHERE instancestatus = 'REGISTERED' OR instancestatus = 'UNREGISTERED';

UPDATE instancemetadata SET statusReason = hostmetadata.statusreason
FROM hostmetadata WHERE hostmetadata.hostname = instancemetadata.discoveryfqdn;

UPDATE instancemetadata SET instancestatus = 'SERVICES_HEALTHY'
FROM hostmetadata WHERE hostmetadata.hostname = instancemetadata.discoveryfqdn AND hostmetadata.hostmetadatastate = 'HEALTHY';

UPDATE instancemetadata SET instancestatus = 'SERVICES_UNHEALTHY'
FROM hostmetadata WHERE hostmetadata.hostname = instancemetadata.discoveryfqdn AND hostmetadata.hostmetadatastate = 'UNHEALTHY';

DELETE FROM hostmetadata;

-- //@UNDO
-- SQL to undo the change goes here.

INSERT INTO hostmetadata (hostgroup_id, hostname, hostmetadatastate, statusreason)
SELECT hostgroup.id as hostgroup_id, instancemetadata.discoveryfqdn, 'HEALTHY', instancemetadata.statusreason FROM instancemetadata
INNER JOIN hostgroup ON instancemetadata.instancegroup_id = hostgroup.instancegroup_id
LEFT JOIN hostmetadata ON instancemetadata.discoveryfqdn = hostmetadata.hostname
WHERE instancemetadata.instancestatus IN ('SERVICES_RUNNING', 'SERVICES_HEALTHY', 'STOPPED')
  AND instancemetadata.terminationdate IS NULL
  AND hostmetadata.id IS NULL;

UPDATE hostmetadata SET hostgroup_id = ihh.hostgroupid, hostname = ihh.fqdn, hostmetadatastate = state, statusreason = reason FROM
(SELECT hostgroup.id as hostgroupid, instancemetadata.discoveryfqdn as fqdn, 'HEALTHY' as state, instancemetadata.statusreason as reason FROM instancemetadata
INNER JOIN hostgroup ON instancemetadata.instancegroup_id = hostgroup.instancegroup_id
WHERE instancemetadata.instancestatus IN ('SERVICES_RUNNING', 'SERVICES_HEALTHY', 'STOPPED')
AND instancemetadata.terminationdate IS NULL) as ihh WHERE hostmetadata.hostname = ihh.fqdn;

INSERT INTO hostmetadata (hostgroup_id, hostname, hostmetadatastate, statusreason)
SELECT hostgroup.id as hostgroup_id, instancemetadata.discoveryfqdn, 'UNHEALTHY', instancemetadata.statusreason FROM instancemetadata
INNER JOIN hostgroup ON instancemetadata.instancegroup_id = hostgroup.instancegroup_id
LEFT JOIN hostmetadata ON instancemetadata.discoveryfqdn = hostmetadata.hostname
WHERE instancemetadata.instancestatus IN ('SERVICES_UNHEALTHY')
  AND instancemetadata.terminationdate IS NULL
  AND hostmetadata.id IS NULL;

UPDATE hostmetadata SET hostgroup_id = ihh.hostgroupid, hostname = ihh.fqdn, hostmetadatastate = state, statusreason = reason FROM
(SELECT hostgroup.id as hostgroupid, instancemetadata.discoveryfqdn as fqdn, 'UNHEALTHY' as state, instancemetadata.statusreason as reason FROM instancemetadata
INNER JOIN hostgroup ON instancemetadata.instancegroup_id = hostgroup.instancegroup_id
WHERE instancemetadata.instancestatus IN ('SERVICES_UNHEALTHY', 'WAITING_FOR_REPAIR')
AND instancemetadata.terminationdate IS NULL) as ihh WHERE hostmetadata.hostname = ihh.fqdn;

UPDATE instancemetadata SET instancestatus = 'REGISTERED' WHERE instancestatus = 'SERVICES_RUNNING';
UPDATE instancemetadata SET instancestatus = 'REGISTERED' WHERE instancestatus = 'SERVICES_HEALTHY';
UPDATE instancemetadata SET instancestatus = 'REGISTERED' WHERE instancestatus = 'SERVICES_UNHEALTHY';