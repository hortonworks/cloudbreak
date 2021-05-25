-- // CB-12504 Add account level databus credential
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS accountdatabusconfig_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE IF NOT EXISTS account_databus_config (
    id                  bigint PRIMARY KEY,
    name                character varying(255) NOT NULL,
    accountid           varchar(255) NOT NULL,
    databuscredential   text NOT NULL,
    CONSTRAINT uk_account_databus_config_name_accountid UNIQUE (name, accountid)
);

ALTER TABLE account_databus_config
   ALTER COLUMN id SET DEFAULT nextval ('accountdatabusconfig_id_seq');

CREATE UNIQUE INDEX IF NOT EXISTS account_databus_config_id_idx ON account_databus_config USING btree (id);
CREATE INDEX IF NOT EXISTS account_databus_config_accountid_name_idx ON account_databus_config USING btree (accountid, name);
CREATE INDEX IF NOT EXISTS account_databus_config_name_idx ON account_databus_config USING btree (name);
CREATE INDEX IF NOT EXISTS account_databus_config_accountid_idx ON account_databus_config USING btree (accountid);

-- //@UNDO
-- SQL to undo the change goes here.
DROP INDEX IF EXISTS account_databus_config_id_idx;
DROP INDEX IF EXISTS account_databus_config_accountid_name_idx;
DROP INDEX IF EXISTS account_databus_config_name_idx;
DROP INDEX IF EXISTS account_databus_config_accountid_idx;

DROP SEQUENCE IF EXISTS accountdatabusconfig_id_seq;
DROP TABLE IF EXISTS account_databus_config;
