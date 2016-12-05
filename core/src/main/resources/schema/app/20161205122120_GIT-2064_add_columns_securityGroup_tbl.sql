-- // Git issue 2064 added two columns securitygroupId and cloudplatform to securitygroup table.
-- Migration SQL that makes the change goes here.

ALTER TABLE securitygroup ADD COLUMN securitygroupid CHARACTER VARYING (255);
ALTER TABLE securitygroup ADD COLUMN cloudplatform CHARACTER VARYING (255);

UPDATE securitygroup SET cloudplatform='AWS' where cloudplatform is NULL;
-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE securitygroup DROP COLUMN IF EXISTS securitygroupid;
ALTER TABLE securitygroup DROP COLUMN IF EXISTS cloudplatform;
