-- // CLOUD-63155_ldap_config
-- Migration SQL that makes the change goes here.

CREATE TABLE ldapconfig
(
  id bigint NOT NULL,
  name varchar(255) NOT NULL,
  description text,
  account varchar(255),
  owner varchar(255),
  publicinaccount boolean NOT NULL,
  serverhost varchar(255) NOT NULL,
  serverport integer NOT NULL,
  serverssl boolean,
  binddn varchar(255),
  bindpassword varchar(255),
  usersearchbase varchar(255) NOT NULL,
  usersearchfilter varchar(255),
  groupsearchbase varchar(255),
  groupsearchfilter varchar(255),
  principalregex varchar(255),

  PRIMARY KEY (id)
);

ALTER TABLE ldapconfig ADD CONSTRAINT uk_ldapconfig_account_name UNIQUE (account, name);

CREATE SEQUENCE ldapconfig_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER TABLE cluster ADD COLUMN ldapconfig_id BIGINT REFERENCES ldapconfig(id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS ldapconfig_id;

DROP TABLE IF EXISTS ldapconfig;

DROP SEQUENCE IF EXISTS ldapconfig_id_seq;
