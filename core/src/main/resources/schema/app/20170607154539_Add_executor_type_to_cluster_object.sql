-- // Add executor type to cluster object
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN executortype TEXT DEFAULT 'SIMPLE';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN executortype;

