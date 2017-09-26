-- // CLOUD-87001 create separated domain for sshkeys
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS stackauthentication
(
    id              bigint NOT NULL,
    publickey       text,
    publickeyid     CHARACTER VARYING (255),
    loginusername   CHARACTER VARYING (255) NOT NULL,
    temp_stackid    BIGINT NOT NULL
);
CREATE SEQUENCE stackauthentication_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER TABLE stackauthentication ADD CONSTRAINT stackauthentication_pkey PRIMARY KEY (id);
ALTER TABLE stack ADD COLUMN stackauthentication_id BIGINT;
ALTER TABLE ONLY stack ADD CONSTRAINT fk_stack_stackauthentication_id FOREIGN KEY (stackauthentication_id) REFERENCES stackauthentication(id);
UPDATE stack SET loginusername = 'cloudbreak' WHERE loginusername IS NULL;
INSERT INTO stackauthentication (id, publickey, loginusername, temp_stackid) SELECT nextval('stackauthentication_id_seq') AS "id", publickey, loginusername, id FROM stack;
UPDATE stack SET stackauthentication_id=o.id FROM stackauthentication o WHERE o.temp_stackid=stack.id;
ALTER TABLE ONLY stackauthentication DROP COLUMN temp_stackid;
-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS stackauthentication_id;
ALTER TABLE stack ADD COLUMN IF NOT EXISTS loginusername text;
ALTER TABLE stack ADD COLUMN IF NOT EXISTS publickey text;
DROP TABLE IF EXISTS stackauthentication;
DROP SEQUENCE IF EXISTS stackauthentication_id_seq;