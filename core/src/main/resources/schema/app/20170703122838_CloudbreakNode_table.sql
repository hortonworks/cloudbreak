-- // CloudbreakNode table
-- Migration SQL that makes the change goes here.

CREATE TABLE cloudbreaknode (
    uuid  VARCHAR(255) NOT NULL,
    lastupdated BIGINT NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (uuid)
);

ALTER TABLE flowlog ADD COLUMN cloudbreakNodeId VARCHAR(255);
ALTER TABLE flowlog ADD COLUMN version BIGINT;

-- //@UNDO
-- SQL to undo the change goes here.


DROP TABLE cloudbreaknode;

ALTER TABLE flowlog DROP COLUMN cloudbreakNodeId;
ALTER TABLE flowlog DROP COLUMN version;
