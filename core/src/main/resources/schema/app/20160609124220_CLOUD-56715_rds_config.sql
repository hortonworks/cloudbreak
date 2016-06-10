-- // CLOUD-56715 rds config
-- Migration SQL that makes the change goes here.

CREATE TABLE rdsconfig
(
   id             BIGINT NOT NULL,
   connectionurl        VARCHAR (255),
   connectionusername   VARCHAR (255),
   connectionpassword   VARCHAR (255),
   databasetype         VARCHAR (255),
   PRIMARY KEY (id)
);

CREATE SEQUENCE rdsconfig_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER TABLE cluster ADD COLUMN rdsconfig_id BIGINT REFERENCES rdsconfig(id);


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS rdsconfig_id;

DROP TABLE IF EXISTS rdsconfig;

DROP SEQUENCE  IF EXISTS  rdsconfig_id_seq;