-- // BUG-102208 remove nonnull from nodecount
-- Migration SQL that makes the change goes here.

ALTER TABLE hostgroup_constraint ALTER hostcount DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE hostgroup_constraint SET hostcount = result.nc FROM (SELECT COUNT(instancemetadata) as nc, nullnodegroup.instancegroup_id FROM
  (SELECT * FROM hostgroup_constraint WHERE hostcount IS NULL) as nullnodegroup, instancemetadata
WHERE nullnodegroup.instancegroup_id = instancemetadata.instancegroup_id AND instancemetadata.instancestatus != 'TERMINATED' GROUP BY nullnodegroup.instancegroup_id) as result
WHERE result.instancegroup_id = hostgroup_constraint.instancegroup_id;

UPDATE hostgroup_constraint SET hostcount = 0 WHERE hostcount IS NULL;

ALTER TABLE hostgroup_constraint ALTER COLUMN hostcount SET NOT NULL;