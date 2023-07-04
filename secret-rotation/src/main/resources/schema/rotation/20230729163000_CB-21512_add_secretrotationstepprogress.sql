-- // secret rotation step progress
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS secretrotationstepprogress (
    id bigserial NOT NULL,
    created bigint,
    finished bigint,
    resourcecrn character varying(255),
    secrettype character varying(255),
    secretrotationstep character varying(255),
    executiontype character varying(255),
    PRIMARY KEY (id)
);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS secretrotationstepprogress;