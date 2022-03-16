-- // CB-1210 create environment table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS consumption (
    id                      bigserial NOT NULL,
    name                    character varying(255) NOT NULL,
    description             text,
    accountid               varchar(255) NOT NULL,
    resourceCrn              varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS consumption_accountid_name_idx ON consumption USING btree (accountid, name);
CREATE INDEX IF NOT EXISTS consumption_id_accountid_idx ON consumption USING btree (id, accountid);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS consumption_id_accountid_idx;
DROP INDEX IF EXISTS idx_consumption_accountid_name;
DROP TABLE IF EXISTS consumption;