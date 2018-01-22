-- // RMP-10380 add Ambari security master key field to Cluster
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD ambarisecuritymasterkey TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS ambarisecuritymasterkey;


