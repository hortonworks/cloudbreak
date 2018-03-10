-- // BUG-97313 Create proxy domain to auto configure Ambari
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS proxyconfig_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE  IF NOT EXISTS proxyconfig (
  id bigint PRIMARY KEY DEFAULT nextval('proxyconfig_id_seq'),
  account character varying(255) NOT NULL,
  owner character varying(255) NOT NULL,
  name character varying(255) NOT NULL,
  serverhost character varying(255) NOT NULL,
  serverport integer NOT NULL,
  protocol character varying(255) NOT NULL,
  username character varying(255),
  password character varying(255),
  publicinaccount boolean NOT NULL
);

ALTER TABLE cluster ADD COLUMN proxyconfig_id BIGINT REFERENCES proxyconfig(id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS proxyconfig_id;

DROP TABLE IF EXISTS proxyconfig;

DROP SEQUENCE IF EXISTS proxyconfig_id_seq;

