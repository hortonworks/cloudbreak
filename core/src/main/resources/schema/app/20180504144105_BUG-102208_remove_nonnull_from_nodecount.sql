-- // BUG-102208 remove nonnull from nodecount
-- Migration SQL that makes the change goes here.

ALTER TABLE instancegroup ALTER nodecount DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE instancegroup SET nodecount = result.nc FROM (SELECT COUNT(instancemetadata) as nc, nullnodegroup.id FROM
  (SELECT * FROM instancegroup WHERE nodecount IS NULL) as nullnodegroup, instancemetadata
WHERE nullnodegroup.id = instancemetadata.instancegroup_id AND instancemetadata.instancestatus != 'TERMINATED' GROUP BY nullnodegroup.id) as result
WHERE result.id = instancegroup.id;

UPDATE instancegroup SET nodecount = 0 WHERE nodecount IS NULL;

ALTER TABLE instancegroup ALTER COLUMN nodecount SET NOT NULL;