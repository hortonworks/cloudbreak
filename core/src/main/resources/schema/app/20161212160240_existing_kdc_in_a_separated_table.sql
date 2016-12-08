-- // existing kdc in a separated table
-- Migration SQL that makes the change goes here.

CREATE TABLE kerberosconfig
(
   id             BIGINT NOT NULL,
   kerberosmasterkey    VARCHAR (255),
   kerberosadmin        VARCHAR (255),
   kerberospassword     VARCHAR (255),
   kerberosurl          VARCHAR (255),
   kerberosrealm        VARCHAR (255),
   kerberosdomain       VARCHAR (255),
   kerberosprincipal    VARCHAR (255),
   PRIMARY KEY (id)
);

CREATE SEQUENCE kerberosconfig_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER TABLE cluster ADD COLUMN kerberosconfig_id bigint;
ALTER TABLE ONLY cluster ADD CONSTRAINT fk_cluster_kerberosconfig_id FOREIGN KEY (kerberosconfig_id) REFERENCES kerberosconfig(id);

ALTER TABLE cluster DROP COLUMN IF EXISTS kerberosmasterkey;
ALTER TABLE cluster DROP COLUMN IF EXISTS kerberosadmin;
ALTER TABLE cluster DROP COLUMN IF EXISTS kerberospassword;
ALTER TABLE cluster DROP COLUMN IF EXISTS kerberosurl;
ALTER TABLE cluster DROP COLUMN IF EXISTS kerberosrealm;
ALTER TABLE cluster DROP COLUMN IF EXISTS kerberosdomain;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY cluster DROP CONSTRAINT fk_cluster_kerberosconfig_id;
ALTER TABLE ONLY cluster DROP COLUMN kerberosconfig_id;
DROP TABLE IF EXISTS kerberosconfig;

DROP SEQUENCE  IF EXISTS  kerberosconfig_id_seq;

ALTER TABLE cluster ADD COLUMN kerberosmasterkey VARCHAR(255);
ALTER TABLE cluster ADD COLUMN kerberosadmin VARCHAR(255);
ALTER TABLE cluster ADD COLUMN kerberospassword VARCHAR(255);
ALTER TABLE cluster ADD COLUMN kerberosurl VARCHAR(255);
ALTER TABLE cluster ADD COLUMN kerberosrealm VARCHAR(255);
ALTER TABLE cluster ADD COLUMN kerberosdomain VARCHAR(255);
