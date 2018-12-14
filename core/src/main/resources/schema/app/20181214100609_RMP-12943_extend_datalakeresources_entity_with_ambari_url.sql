-- // RMP-12943 extend datalakeresources entity with ambari url
-- Migration SQL that makes the change goes here.

ALTER TABLE datalakeresources ADD COLUMN IF NOT EXISTS datalakeambariurl VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE datalakeresources DROP COLUMN IF EXISTS datalakeambariurl;
