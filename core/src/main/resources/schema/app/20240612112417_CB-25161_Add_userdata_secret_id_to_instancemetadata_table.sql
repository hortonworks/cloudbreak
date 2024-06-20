-- // CB-25161 Add userdata secret id to instancemetadata table
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata ADD COLUMN IF NOT EXISTS userdatasecretresource_id BIGINT
    CONSTRAINT fk_instancemetadata_userdatasecretresource_id REFERENCES resource;
ALTER TABLE archivedinstancemetadata ADD COLUMN IF NOT EXISTS userdatasecretresource_id BIGINT;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancemetadata DROP COLUMN IF EXISTS userdatasecretresource_id;
ALTER TABLE archivedinstancemetadata DROP COLUMN IF EXISTS userdatasecretresource_id;
