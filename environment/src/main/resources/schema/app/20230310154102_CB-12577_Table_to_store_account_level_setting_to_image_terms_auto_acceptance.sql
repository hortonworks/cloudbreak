-- // CB-12577 Table to store account level setting to image terms auto acceptance
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS terms (
    id                  bigserial NOT NULL,
    accepted            bool DEFAULT false,
    accountid           varchar(255) NOT NULL,
    resourcecrn         varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS terms_id_idx ON terms USING btree (id);
CREATE INDEX IF NOT EXISTS terms_accountid_idx ON terms USING btree (accountid);
CREATE INDEX IF NOT EXISTS terms_id_accountid_idx ON terms USING btree (id, accountid);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS terms_id_idx;
DROP INDEX IF EXISTS terms_accountid_idx;
DROP INDEX IF EXISTS terms_id_accountid_idx;

DROP TABLE IF EXISTS terms;
