-- // CB-9366 Create load balancer metadata table
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE loadbalancer_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE loadbalancer (
    id bigint default nextval('loadbalancer_id_seq'::regclass) not null
      constraint loadbalancer_pkey
        primary key,
    dns character varying(255),
    hostedzoneid character varying(255),
    ip character varying(255),
    type character varying(255),
    endpoint character varying(255),
    stack_id bigint
      constraint fk_loadbalancer_stack_id
        references stack
);

CREATE INDEX loadbalancer_stack_id on loadbalancer (stack_id);

CREATE SEQUENCE targetgroup_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE targetgroup (
    id bigint default nextval('targetgroup_id_seq'::regclass) not null
      constraint targetgroup_pkey
        primary key,
    type character varying(255),
    loadbalancer_id bigint
      constraint fk_targetgroup_loadbalancer_id
          references loadbalancer
);

CREATE TABLE targetgroup_instancegroup (
    targetgroups_id bigint NOT NULL,
    instancegroups_id bigint NOT NULL
);

ALTER TABLE ONLY targetgroup_instancegroup ADD CONSTRAINT targetgroup_instancegroup_pkey PRIMARY KEY (targetgroups_id, instancegroups_id);
ALTER TABLE ONLY targetgroup_instancegroup ADD CONSTRAINT fk_targetgroup_instancegroup_targetgroup_id FOREIGN KEY (targetgroups_id) REFERENCES targetgroup(id);
ALTER TABLE ONLY targetgroup_instancegroup ADD CONSTRAINT fk_targetgroup_instancegroup_instancegroup_id FOREIGN KEY (instancegroups_id) REFERENCES instancegroup(id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE targetgroup_instancegroup;

DROP TABLE targetgroup;

DROP SEQUENCE targetgroup_id_seq;

DROP TABLE loadbalancer;

DROP SEQUENCE loadbalancer_id_seq;
