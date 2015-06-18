-- // CLOUD-399_store_cert_dir_in_stack
-- Migration SQL that makes the change goes here.

CREATE TABLE securityconfig (
    id bigint NOT NULL,
    clientKey text,
    clientCert text,
    serverCert text,
    temporarySshPublicKey text,
    temporarySshPrivateKey text,
    stack_id bigint NOT NULL
);

ALTER TABLE ONLY securityconfig ADD CONSTRAINT securityconfig_pkey PRIMARY KEY (id);

ALTER TABLE ONLY securityconfig ADD CONSTRAINT fk_securityconfig_stack_id FOREIGN KEY (stack_id) REFERENCES stack(id);

CREATE SEQUENCE securityconfig_table START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE securityconfig;
DROP SEQUENCE securityconfig_table;
