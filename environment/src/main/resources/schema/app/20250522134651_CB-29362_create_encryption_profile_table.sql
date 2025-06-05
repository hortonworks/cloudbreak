-- // CB-29362 create encryption profile table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS encryptionprofile (
    id                  bigserial NOT NULL,
    name                varchar(255) NOT NULL,
    description         text,
    tls_versions        varchar(255) NOT NULL,
    cipher_suites       text,
    resourcecrn         varchar(255) NOT NULL,
    accountid           varchar(255) NOT NULL,
    created             bigint DEFAULT '-1'::integer,
    archived            bool DEFAULT false,
    CONSTRAINT encryptionprofile_name_accountid_unique UNIQUE (name, accountid),
    PRIMARY KEY (id)
    );

CREATE UNIQUE INDEX IF NOT EXISTS encryptionprofile_id_idx ON encryptionprofile USING btree (id);
CREATE INDEX IF NOT EXISTS encryptionprofile_name_idx ON encryptionprofile USING btree (name);
CREATE INDEX IF NOT EXISTS encryptionprofile_accountid_idx ON encryptionprofile USING btree (accountid);
CREATE INDEX IF NOT EXISTS encryptionprofile_resourcecrn_idx ON encryptionprofile USING btree (resourcecrn);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS encryptionprofile_id_idx;
DROP INDEX IF EXISTS encryptionprofile_name_idx;
DROP INDEX IF EXISTS encryptionprofile_accountid_idx;
DROP INDEX IF EXISTS encryptionprofile_resourcecrn_idx;

DROP TABLE IF EXISTS encryptionprofile;
