-- // CLOUD-50403 add container table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS container
(
    id                  BIGINT NOT NULL,
    containerId         CHARACTER VARYING (255) NOT NULL,
    name                CHARACTER VARYING (255) NOT NULL,
    image               CHARACTER VARYING (255) NOT NULL,
    host                CHARACTER VARYING (255) NOT NULL,
    cluster_id          BIGINT NOT NULL
);

CREATE SEQUENCE container_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER TABLE ONLY container
    ADD CONSTRAINT container_pkey PRIMARY KEY (id);

ALTER TABLE ONLY container
    ADD CONSTRAINT fk_container_cluster_id FOREIGN KEY (cluster_id) REFERENCES cluster(id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP SEQUENCE IF EXISTS container_id_seq;

DROP TABLE IF EXISTS container;
