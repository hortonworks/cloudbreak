-- // CB-29063 Secret rotation should store lastupdated timestamp when finished
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS secretrotationhistory (
    id bigserial NOT NULL,
    resourcecrn character varying(255),
    secrettype text,
    lastupdated bigint,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_secretrotationhistory_crn_secrettype
    ON secretrotationhistory
    USING btree (resourcecrn, secrettype);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_secretrotationhistory_crn_secrettype;
DROP TABLE IF EXISTS secretrotationhistory;