-- // CB-16553 - archive table for unused clustercomponents and its audited histories
-- Migration SQL that makes the change goes here.
CREATE TABLE IF NOT EXISTS archiveclustercomponent (
    id                  bigint          NOT NULL,
    componenttype       varchar(63)     NULL,
    name                varchar(255)    NULL,
    cluster_id          int8            NULL,
    attributes          text            NULL
);

ALTER TABLE archiveclustercomponent ADD CONSTRAINT archiveclustercomponent_pkey PRIMARY KEY (id);

-- //@UNDO
-- SQL to undo the change goes here.

INSERT INTO clustercomponent SELECT id, componenttype, name, cluster_id, attributes FROM archiveclustercomponent;

DROP TABLE IF EXISTS archiveclustercomponent;
