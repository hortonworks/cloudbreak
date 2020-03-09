-- // CDPCP-1533 Add Operation to usersync status. The lastfullsyncstarttime
-- // and lastfullsyncendtime columns dropped because they are redundant with
-- // the information stored in lastrequestedfullsync and lastsuccessfulfullsync.

ALTER TABLE usersyncstatus
    ADD COLUMN IF NOT EXISTS lastrequestedfullsync_id bigint
        CONSTRAINT fk_usersyncstatus_lastrequestedoperationid
        REFERENCES operation,
    ADD COLUMN IF NOT EXISTS lastsuccessfulfullsync_id bigint
        CONSTRAINT fk_usersyncstatus_lastsuccessfuloperationid
        REFERENCES operation,
    DROP COLUMN IF EXISTS lastfullsyncstarttime,
    DROP COLUMN IF EXISTS lastfullsyncendtime;

-- //@UNDO

ALTER TABLE usersyncstatus
    ADD COLUMN IF NOT EXISTS lastfullsyncstarttime bigint,
    ADD COLUMN IF NOT EXISTS lastfullsyncendtime bigint,
    DROP COLUMN IF EXISTS lastrequestedfullsync_id,
    DROP COLUMN IF EXISTS lastsuccessfulfullsync_id;

COMMENT on COLUMN usersyncstatus.lastfullsyncstarttime is 'UTC milliseconds from java epoch';
COMMENT on COLUMN usersyncstatus.lastfullsyncendtime is 'UTC milliseconds from java epoch';
