-- // CB-1210 create credential table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS accounttag (
    id                  bigserial NOT NULL,
    tagkey              varchar(255) NOT NULL,
    tagvalue            varchar(255) NOT NULL,
    archived            bool DEFAULT false,
    accountid           varchar(255) NOT NULL,
    resourcecrn         varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS accounttag_id_idx ON accounttag USING btree (id);
CREATE INDEX IF NOT EXISTS accounttag_accountid_idx ON accounttag USING btree (accountid);
CREATE INDEX IF NOT EXISTS accounttag_id_accountid_idx ON accounttag USING btree (id, accountid);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS accounttag_id_idx;
DROP INDEX IF EXISTS accounttag_accountid_idx;
DROP INDEX IF EXISTS accounttag_id_accountid_idx;

DROP TABLE IF EXISTS accounttag;