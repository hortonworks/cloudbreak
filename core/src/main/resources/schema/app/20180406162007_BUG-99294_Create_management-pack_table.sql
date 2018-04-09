-- // BUG-99294 Create management-pack table
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS managementpack_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE  IF NOT EXISTS managementpack (
  id bigint PRIMARY KEY DEFAULT nextval('managementpack_id_seq'),
  account character varying(255) NOT NULL,
  owner character varying(255) NOT NULL,
  name character varying(255) NOT NULL,
  description text,
  mpackurl character varying(255) NOT NULL,
  purge boolean,
  purgelist character varying(255),
  force boolean,
  publicinaccount boolean NOT NULL
);

ALTER TABLE managementpack ADD CONSTRAINT uk_managementpack_account_name UNIQUE (account, name);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE managementpack DROP CONSTRAINT IF EXISTS uk_managementpack_account_name;

DROP TABLE IF EXISTS managementpack;

DROP SEQUENCE IF EXISTS managementpack_id_seq;
