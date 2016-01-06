-- // CLOUD-49844 Ability to store Cloud Platform topology in Cloudbreak
-- Migration SQL that makes the change goes here.


CREATE SEQUENCE topology_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE topology (
    id              bigint NOT NULL,
    account         character varying(255) NOT NULL,
    owner           character varying(255),
    name            character varying(255) NOT NULL,
    description     TEXT,
    cloudplatform   character varying(255) NOT NULL,
    endpoint        character varying(255),
    deleted boolean DEFAULT FALSE
);

ALTER TABLE topology
    ADD CONSTRAINT topology_pkey PRIMARY KEY (id);

ALTER TABLE topology
   ADD CONSTRAINT uk_topology_name UNIQUE (account, name);

CREATE TABLE topology_records (
    hypervisor  TEXT NOT NULL,
    rack        TEXT NOT NULL,
    topology_id bigint NOT NULL
);

ALTER TABLE topology_records
   ADD CONSTRAINT fk_topology_records_topology FOREIGN KEY (topology_id)
       REFERENCES topology (id);

-- //@UNDO
-- SQL to undo the change goes here.

drop table topology_records;
drop table topology;
drop sequence topology_id_seq;
