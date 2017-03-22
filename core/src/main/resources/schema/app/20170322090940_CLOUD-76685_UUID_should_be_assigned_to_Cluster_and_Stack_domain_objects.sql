-- // CLOUD-76685 UUID should be assigned to Cluster and Stack domain objects
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN uuid character varying(255);
ALTER TABLE cloudbreakusage ADD COLUMN ParentUuid character varying(255);


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS uuid;
ALTER TABLE cloudbreakusage DROP COLUMN IF EXISTS ParentUuid;
