-- // CB-23382 create periscope job table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS periscopejob (
	jobname text PRIMARY KEY,
	lastexecuted bigint
);

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS deleteRetryCount int;



-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS periscopejob;
ALTER TABLE cluster DROP COLUMN IF EXISTS deleteRetryCount;



