-- // RMP-11205_Periscope_node_heartbeat
-- Migration SQL that makes the change goes here.


CREATE TABLE periscopenode (
    uuid  VARCHAR(255) NOT NULL,
    leader boolean NOT NULL DEFAULT false,
    lastupdated BIGINT NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (uuid)
);

CREATE INDEX ix_periscopenode_isleader_lastupdated ON periscopenode USING btree (leader, lastupdated);

ALTER TABLE cluster ADD COLUMN periscopenodeid VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS periscopenode;

ALTER TABLE cluster DROP COLUMN IF EXISTS periscopenodeid;