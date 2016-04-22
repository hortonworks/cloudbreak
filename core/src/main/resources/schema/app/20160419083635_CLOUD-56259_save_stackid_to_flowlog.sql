-- // CLOUD-56259 save stack id to flowlog and flag finalized flows
-- Migration SQL that makes the change goes here.

ALTER TABLE flowlog
    ADD COLUMN stackid bigint,
    ADD COLUMN finalized bool;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE flowlog
    DROP COLUMN stackid,
    DROP COLUMN finalized;
