-- // CLOUD-59100 remove hostservice table
-- Migration SQL that makes the change goes here.

DROP SEQUENCE IF EXISTS hostservice_id_seq;

DROP TABLE IF EXISTS hostservice;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE IF NOT EXISTS hostservice
(
    id                  BIGINT NOT NULL,
    name                CHARACTER VARYING (255) NOT NULL,
    host                CHARACTER VARYING (255) NOT NULL,
    cluster_id          BIGINT NOT NULL
);

CREATE SEQUENCE hostservice_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER TABLE ONLY hostservice
    ADD CONSTRAINT hostservice_pkey PRIMARY KEY (id);

ALTER TABLE ONLY hostservice
    ADD CONSTRAINT fk_hostservice_cluster_id FOREIGN KEY (cluster_id) REFERENCES cluster(id);

-- // CLOUD-56183 added hostservice table
-- Migration SQL that makes the change goes here.