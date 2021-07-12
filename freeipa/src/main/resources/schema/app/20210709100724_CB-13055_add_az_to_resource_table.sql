-- // CB-13055 add az to resource table
-- Migration SQL that makes the change goes here.

ALTER TABLE resource ADD COLUMN IF NOT EXISTS availabilityzone VARCHAR(1000);
UPDATE resource SET availabilityzone=(SELECT availabilityzone FROM stack WHERE id=resource.resource_stack);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE resource DROP COLUMN IF EXISTS availabilityzone;

