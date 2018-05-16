-- // RMP-11205_Periscope_cluster_last_evailation
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN lastevaulated BIGINT DEFAULT 0;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS lastevaulated;