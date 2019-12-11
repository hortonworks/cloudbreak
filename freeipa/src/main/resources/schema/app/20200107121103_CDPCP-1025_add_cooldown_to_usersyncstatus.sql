-- // CDPCP-1025 track the last time usersync is requested
-- Migration SQL that makes the change goes here.

ALTER TABLE usersyncstatus
    ADD COLUMN IF NOT EXISTS lastfullsyncstarttime bigint,
    ADD COLUMN IF NOT EXISTS lastfullsyncendtime bigint;

COMMENT on COLUMN usersyncstatus.lastfullsyncstarttime is 'UTC milliseconds from java epoch';
COMMENT on COLUMN usersyncstatus.lastfullsyncendtime is 'UTC milliseconds from java epoch';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE usersyncstatus
    DROP COLUMN IF EXISTS lastfullsyncstarttime,
    DROP COLUMN IF EXISTS lastfullsyncendtime;
