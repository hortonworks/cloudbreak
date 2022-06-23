-- // CB-17551 Deprecate initiatorusercrn
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ALTER COLUMN initiatorUserCrn DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster ALTER COLUMN initiatorUserCrn SET NOT NULL;


