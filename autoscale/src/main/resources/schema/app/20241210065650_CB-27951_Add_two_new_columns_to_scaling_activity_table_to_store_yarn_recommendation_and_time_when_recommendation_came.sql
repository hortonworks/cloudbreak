-- // CB-27951 Add two new columns to scaling activity table to store yarn recommendation and time when recommendation came
-- Migration SQL that makes the change goes here.
ALTER TABLE scaling_activity ADD COLUMN IF NOT EXISTS yarn_recommendation_time timestamp;

ALTER TABLE scaling_activity ADD COLUMN IF NOT EXISTS yarn_recommendation text;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE scaling_activity DROP COLUMN IF EXISTS yarn_recommendation_time;

ALTER TABLE scaling_activity DROP COLUMN IF EXISTS yarn_recommendation;
