-- // DPAAS-81 add token topology cert for hierarchical Knox topologies
-- Migration SQL that makes the change goes here.

ALTER TABLE gateway ADD COLUMN tokencert TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE gateway DROP COLUMN tokencert;


