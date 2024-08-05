-- // CB-26102 create table for load balancer
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS loadbalancer_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS loadbalancer (
    id                BIGINT PRIMARY KEY NOT NULL,
    stack_id          BIGINT NOT NULL,
    resource_id       VARCHAR(255),
    ip                VARCHAR(256),
    fqdn              VARCHAR(255),
    hosted_zone_id    VARCHAR(255),
    endpoint          VARCHAR(255),
    dns               VARCHAR(255),
    CONSTRAINT fk_lb_stackid FOREIGN KEY (stack_id) REFERENCES stack(id)
);

CREATE INDEX IF NOT EXISTS idx_loadbalancer_stack_id
  ON loadbalancer (stack_id);

CREATE SEQUENCE IF NOT EXISTS targetgroup_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS targetgroup (
    id                BIGINT PRIMARY KEY NOT NULL,
    loadbalancer_id   BIGINT,
    traffic_port      INTEGER,
    protocol          VARCHAR(255),
    CONSTRAINT fk_tg_lb FOREIGN KEY (loadbalancer_id) REFERENCES loadbalancer(id)
);

CREATE INDEX IF NOT EXISTS idx_targetgroup_loadbalancer_id
  ON targetgroup (loadbalancer_id);

-- //@UNDO
-- SQL to undo the change goes here.
DROP INDEX IF EXISTS idx_targetgroup_loadbalancer_id;
DROP TABLE IF EXISTS targetgroup;
DROP SEQUENCE IF EXISTS targetgroup_id_seq;
DROP INDEX IF EXISTS idx_loadbalancer_stack_id;
DROP TABLE IF EXISTS loadbalancer;
DROP SEQUENCE IF EXISTS loadbalancer_id_seq;
