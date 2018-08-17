-- // BUG-108475
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE cluster ALTER COLUMN account DROP NOT NULL;
ALTER TABLE cluster DROP CONSTRAINT IF EXISTS uk_cluster_account_name;

-- //@UNDO
-- SQL to undo the change goes here.


