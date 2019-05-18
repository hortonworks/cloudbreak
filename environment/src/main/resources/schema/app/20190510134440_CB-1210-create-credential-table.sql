-- // CB-1210 create credential table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS credential (
    id                  bigserial NOT NULL,
    description         text,
    name                varchar(255) NOT NULL,
    publickey           text,
    archived            bool DEFAULT false,
    loginusername       text,
    attributes          text,
    cloudplatform       varchar(255) NOT NULL,
    accountid           varchar(255) NOT NULL,
    govcloud            bool DEFAULT false,
    CONSTRAINT credentialname_in_accountid_unique UNIQUE (name, accountid),
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS credential_id_idx ON credential USING btree (id);
CREATE INDEX IF NOT EXISTS credential_name_idx ON credential USING btree (name);
CREATE INDEX IF NOT EXISTS credential_accountid_idx ON credential USING btree (accountid);
CREATE INDEX IF NOT EXISTS credential_id_accountid_idx ON credential USING btree (id, accountid);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS credential_id_idx;
DROP INDEX IF EXISTS credential_name_idx;
DROP INDEX IF EXISTS credential_accountid_idx;
DROP INDEX IF EXISTS credential_id_accountid_idx;

DROP TABLE IF EXISTS credential;