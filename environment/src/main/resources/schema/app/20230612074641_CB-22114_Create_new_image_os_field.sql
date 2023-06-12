-- // CB-12577 Table to store account level setting to image terms auto acceptance
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS freeipaimageos varchar(255);


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS freeipaimageos;