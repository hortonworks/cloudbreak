-- // Rename repair flow chain id
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS lastcbflowchainid character varying(255);
UPDATE sdxcluster SET lastcbflowchainid = repairflowchainid;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE sdxcluster SET repairflowchainid = lastcbflowchainid;
ALTER TABLE sdxcluster DROP COLUMN IF EXISTS lastcbflowchainid;