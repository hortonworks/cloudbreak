-- // CB-9366 Create load balancer metadata table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS loadbalancer (
    id              bigserial NOT NULL,
    dns             character varying(255),
    hostedzoneid    character varying(255),
    ip              character varying(255),
    type            character varying(255),
    endpoint        character varying(255),
    stack_id        bigint
      constraint fk_loadbalancer_stack_id
        references stack,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS loadbalancer_id_idx ON loadbalancer USING btree (id);
CREATE INDEX IF NOT EXISTS loadbalancer_stack_id ON loadbalancer (stack_id);

CREATE TABLE IF NOT EXISTS targetgroup (
    id                  bigserial NOT NULL,
    type                character varying(255),
    loadbalancer_id     bigint
      constraint fk_targetgroup_loadbalancer_id
        references loadbalancer,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS targetgroup_id_idx ON targetgroup USING btree (id);
CREATE INDEX IF NOT EXISTS targetgroup_loadbalancer_id ON targetgroup (loadbalancer_id);

CREATE TABLE IF NOT EXISTS targetgroup_instancegroup (
    targetgroups_id     bigint NOT NULL,
    instancegroups_id   bigint NOT NULL
);

ALTER TABLE ONLY targetgroup_instancegroup ADD CONSTRAINT targetgroup_instancegroup_pkey PRIMARY KEY (targetgroups_id, instancegroups_id);
ALTER TABLE ONLY targetgroup_instancegroup ADD CONSTRAINT fk_targetgroup_instancegroup_targetgroup_id FOREIGN KEY (targetgroups_id) REFERENCES targetgroup(id);
ALTER TABLE ONLY targetgroup_instancegroup ADD CONSTRAINT fk_targetgroup_instancegroup_instancegroup_id FOREIGN KEY (instancegroups_id) REFERENCES instancegroup(id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS targetgroup_instancegroup;

DROP INDEX IF EXISTS targetgroup_loadbalancer_id;
DROP INDEX IF EXISTS targetgroup_id_idx;
DROP TABLE IF EXISTS targetgroup;

DROP INDEX IF EXISTS loadbalancer_stack_id;
DROP INDEX IF EXISTS loadbalancer_id_idx;
DROP TABLE IF EXISTS loadbalancer;
