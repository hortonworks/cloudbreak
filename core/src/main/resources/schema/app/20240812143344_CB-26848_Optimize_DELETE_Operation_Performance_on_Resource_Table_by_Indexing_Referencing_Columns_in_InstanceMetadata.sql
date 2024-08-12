-- // CB-26848 Optimize DELETE Operation Performance on Resource Table by Indexing Referencing Columns in InstanceMetadata
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_instancemetadata_userdatasecretresource ON instancemetadata(userdatasecretresource_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_instancemetadata_userdatasecretresource;
