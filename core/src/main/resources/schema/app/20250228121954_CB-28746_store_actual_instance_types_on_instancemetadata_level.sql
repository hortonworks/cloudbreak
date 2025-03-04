-- // CB-28746 Store actual instance types on instancemetadata level
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata ADD COLUMN IF NOT EXISTS providerinstancetype varchar(255);
ALTER TABLE archivedinstancemetadata ADD COLUMN IF NOT EXISTS providerinstancetype varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancemetadata DROP COLUMN IF EXISTS providerinstancetype;
ALTER TABLE archivedinstancemetadata DROP COLUMN IF EXISTS providerinstancetype;