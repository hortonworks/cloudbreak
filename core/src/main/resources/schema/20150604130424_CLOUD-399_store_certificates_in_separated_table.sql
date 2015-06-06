-- // CLOUD-399_store_cert_dir_in_stack
-- Migration SQL that makes the change goes here.

CREATE TABLE securityconfig (
    id bigint NOT NULL,
    clientKey text,
    clientCert text,
    serverCert text,
    temporarySshPublicKey text,
    temporarySshPrivateKey text,
    stack_id bigint
);

ALTER TABLE ONLY securityconfig ADD CONSTRAINT securityconfig_pkey PRIMARY KEY (id);

ALTER TABLE ONLY securityconfig ADD CONSTRAINT fk_securityconfig_stack_id FOREIGN KEY (stack_id) REFERENCES stack(id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE securityconfig;
