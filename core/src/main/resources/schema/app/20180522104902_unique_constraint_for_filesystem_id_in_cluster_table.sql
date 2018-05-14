-- // unique_constraint_for_filesystem_id_in_cluster_table
-- Migration SQL that makes the change goes here.
ALTER TABLE cluster
ADD CONSTRAINT unique_filesystem_for_cluster UNIQUE (filesystem_id);


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE cluster
DROP CONSTRAINT IF EXISTS unique_filesystem_for_cluster;

