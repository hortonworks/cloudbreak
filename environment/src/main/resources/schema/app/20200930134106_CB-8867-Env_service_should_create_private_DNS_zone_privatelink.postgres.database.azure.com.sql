-- // CB-8867-Env service should create private DNS zone privatelink.postgres.database.azure.com
-- Migration SQL that makes the change goes here.
CREATE SEQUENCE IF NOT EXISTS resource_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS resource
(
   id                   bigserial NOT NULL ,
   environment_id       bigserial NOT NULL ,
   resourcename         VARCHAR(255) NOT NULL,
   resourcereference    VARCHAR(255),
   resourcetype         VARCHAR(255) NOT NULL,
   resourcestatus       VARCHAR(255) NOT NULL DEFAULT 'CREATED',
   CONSTRAINT fk_resource_resource_environment FOREIGN KEY (environment_id) REFERENCES environment(id),
   CONSTRAINT resource_pkey PRIMARY KEY (id),
   CONSTRAINT uk_resource UNIQUE (resourcename, resourcetype, resourcereference)
);
CREATE INDEX IF NOT EXISTS resource_environment_id_idx ON resource (environment_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS resource_environment_id_idx;
DROP TABLE IF EXISTS resource;
DROP SEQUENCE IF EXISTS resource_id_seq;
