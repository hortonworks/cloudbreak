-- // CB-13174 increase securitygroup length
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS environment ALTER COLUMN securitygroup_id_knox SET DATA TYPE VARCHAR(4000);
ALTER TABLE IF EXISTS environment ALTER COLUMN securitygroup_id_default SET DATA TYPE VARCHAR(4000);

-- //@UNDO
-- SQL to undo the change goes here.


