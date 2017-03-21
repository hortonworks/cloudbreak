-- // CLOUD-77160 Extend usage data in CB for flex
-- Migration SQL that makes the change goes here.

ALTER TABLE cloudbreakusage ADD COLUMN Peak integer;
ALTER TABLE cloudbreakusage ADD COLUMN FlexId character varying(255);
ALTER TABLE cloudbreakusage ADD COLUMN StackUuid character varying(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cloudbreakusage DROP COLUMN IF EXISTS Peak;
ALTER TABLE cloudbreakusage DROP COLUMN IF EXISTS FlexId;
ALTER TABLE cloudbreakusage DROP COLUMN IF EXISTS StackUuid;

