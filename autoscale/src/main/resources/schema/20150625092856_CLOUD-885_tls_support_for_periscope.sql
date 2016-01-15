-- // CLOUD-885_tls_support_for_periscope
-- Migration SQL that makes the change goes here.

CREATE TABLE securityconfig (
    id bigint NOT NULL,
    clientKey bytea,
    clientCert bytea,
    serverCert bytea,
    cluster_id bigint NOT NULL
);

ALTER TABLE ONLY securityconfig ADD CONSTRAINT securityconfig_pkey PRIMARY KEY (id);

ALTER TABLE ONLY securityconfig ADD CONSTRAINT fk_securityconfig_cluster_id FOREIGN KEY (cluster_id) REFERENCES cluster(id);

CREATE SEQUENCE securityconfig_table START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;



-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE securityconfig;
DROP SEQUENCE securityconfig_table;

