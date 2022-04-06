-- // CB-13609 Autoscaling should use machine user to query yarn
-- Migration SQL that makes the change goes here.
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS environment_crn VARCHAR(255);

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS machine_user_crn VARCHAR(255);

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS update_failed_details VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_cluster_environment_crn ON cluster (environment_crn);


-- //@UNDO
-- SQL to undo the change goes here.
DROP INDEX IF EXISTS idx_cluster_environment_crn;

ALTER TABLE cluster DROP COLUMN IF EXISTS environment_crn;

ALTER TABLE cluster DROP COLUMN IF EXISTS machine_user_crn;

ALTER TABLE cluster DROP COLUMN IF EXISTS update_failed_details;


