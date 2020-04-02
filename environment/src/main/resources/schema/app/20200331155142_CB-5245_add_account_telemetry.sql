-- // CB-5245 add account telemetry
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS accounttelemetry (
    id                  bigserial NOT NULL,
    features            text,
    rules               text,
    archived            bool DEFAULT false,
    accountid           varchar(255) NOT NULL,
    resourcecrn         varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS accounttelemetry_id_idx ON accounttelemetry USING btree (id);
CREATE INDEX IF NOT EXISTS accounttelemetry_accountid_idx ON accounttelemetry USING btree (accountid);
CREATE INDEX IF NOT EXISTS accounttelemetry_id_accountid_idx ON accounttelemetry USING btree (id, accountid);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS accounttelemetry_id_idx;
DROP INDEX IF EXISTS accounttelemetry_accountid_idx;
DROP INDEX IF EXISTS accounttelemetry_id_accountid_idx;

DROP TABLE IF EXISTS accounttelemetry;