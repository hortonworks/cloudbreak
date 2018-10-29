-- // move secrets to Vault
-- Migration SQL that makes the change goes here.

ALTER TABLE securityconfig DROP COLUMN IF EXISTS cloudbreakSshPrivateKey;
ALTER TABLE securityconfig DROP COLUMN IF EXISTS cloudbreakSshPublicKey;
ALTER TABLE securityconfig DROP COLUMN IF EXISTS temporarysshprivatekey;
ALTER TABLE securityconfig DROP COLUMN IF EXISTS temporarysshpublickey;
ALTER TABLE securityconfig DROP COLUMN IF EXISTS knoxmastersecret;
ALTER TABLE securityconfig DROP COLUMN IF EXISTS saltpassword;
ALTER TABLE securityconfig DROP COLUMN IF EXISTS saltsignpublickey;
ALTER TABLE securityconfig DROP COLUMN IF EXISTS saltsignprivatekey;
ALTER TABLE securityconfig DROP COLUMN IF EXISTS saltbootpassword;
ALTER TABLE securityconfig DROP COLUMN IF EXISTS saltbootsignpublickey;
ALTER TABLE securityconfig DROP COLUMN IF EXISTS saltbootsignprivatekey;

ALTER TABLE gateway ADD COLUMN IF NOT EXISTS knoxmastersecret VARCHAR(255);

CREATE TABLE IF NOT EXISTS saltsecurityconfig
(
   id                       BIGINT NOT NULL,
   saltpassword             VARCHAR (255),
   saltsignpublickey        TEXT,
   saltsignprivatekey       VARCHAR (255),
   saltbootpassword         VARCHAR (255),
   saltbootsignpublickey    TEXT,
   saltbootsignprivatekey   VARCHAR (255),
   PRIMARY KEY (id)
);
CREATE SEQUENCE IF NOT EXISTS saltsecurityconfig_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER TABLE securityconfig ADD COLUMN saltsecurityconfig_id BIGINT;
ALTER TABLE securityconfig ADD CONSTRAINT fk_securityconfig_saltsecurityconfig_id FOREIGN KEY (saltsecurityconfig_id) REFERENCES saltsecurityconfig(id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS cloudbreakSshPrivateKey TEXT;
ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS cloudbreakSshPublicKey TEXT;
ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS temporarysshprivatekey TEXT;
ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS temporarysshpublickey TEXT;
ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS knoxmastersecret VARCHAR(255);
ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS saltpassword VARCHAR (255);
ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS saltsignpublickey TEXT;
ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS saltsignprivatekey TEXT;
ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS saltbootpassword VARCHAR (255);
ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS saltbootsignpublickey TEXT;
ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS saltbootsignprivatekey TEXT;
ALTER TABLE securityconfig DROP COLUMN IF EXISTS saltsecurityconfig_id;

ALTER TABLE gateway DROP COLUMN IF EXISTS knoxmastersecret;

DROP TABLE IF EXISTS saltsecurityconfig;
DROP SEQUENCE IF EXISTS saltsecurityconfig_id_seq;
