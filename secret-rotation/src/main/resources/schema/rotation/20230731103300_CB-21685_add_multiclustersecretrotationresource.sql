-- // CB-21685 tracking multi cluster secret rotation resources
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS multiclusterrotationresource (
    id bigserial NOT NULL,
    resourcecrn character varying(255),
    secrettype text,
    type character varying(255),
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_multiclusterrotationresource_crn_secrettype
    ON multiclusterrotationresource
    USING btree (resourcecrn, secrettype);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_multiclusterrotationresource_crn_secrettype;
DROP TABLE IF EXISTS multiclusterrotationresource;