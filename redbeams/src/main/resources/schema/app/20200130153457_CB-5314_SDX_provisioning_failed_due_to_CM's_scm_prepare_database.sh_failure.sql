-- // CB-5314 SDX provisioning failed due to CM's scm_prepare_database.sh failure
-- Migration SQL that makes the change goes here.

CREATE TABLE resource
(
   id                   BIGINT NOT NULL,
   resource_dbstack     BIGINT NOT NULL CONSTRAINT fk_resource_resource_dbstack
            REFERENCES dbstack,
   resourcename         VARCHAR(255) NOT NULL,
   resourcereference    VARCHAR(255),
   resourcetype         VARCHAR(255) NOT NULL,
   resourcestatus       VARCHAR(255) NOT NULL DEFAULT 'CREATED',
   PRIMARY KEY (id)
);

CREATE SEQUENCE resource_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS resource;
DROP SEQUENCE IF EXISTS resource_id_seq;

