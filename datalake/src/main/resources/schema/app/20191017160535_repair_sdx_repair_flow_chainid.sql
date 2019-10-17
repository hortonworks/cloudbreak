-- // Rename repair flow chain id
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster RENAME COLUMN repairflowchainid TO lastcbflowchainid;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster RENAME COLUMN lastcbflowchainid TO repairflowchainid;