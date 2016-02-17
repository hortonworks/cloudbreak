-- // create_filesystem
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS filesystem
(
    id          bigint NOT NULL,
    name        CHARACTER VARYING (255) NOT NULL,
    type        CHARACTER VARYING (255) NOT NULL,
    defaultfs   boolean NOT NULL
);

CREATE TABLE filesystem_properties (
    filesystem_id bigint NOT NULL,
    value text,
    key character varying(255) NOT NULL
);

CREATE SEQUENCE filesystem_table START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER TABLE ONLY filesystem
    ADD CONSTRAINT filesystem_pkey PRIMARY KEY (id);

ALTER TABLE ONLY filesystem_properties
    ADD CONSTRAINT filesystem_properties_pkey PRIMARY KEY (filesystem_id, key);

ALTER TABLE ONLY filesystem_properties
    ADD CONSTRAINT fk_filesystem_properties_filesystem_id FOREIGN KEY (filesystem_id) REFERENCES filesystem(id);

ALTER TABLE cluster
    ADD COLUMN filesystem_id bigint;

ALTER TABLE ONLY cluster
    ADD CONSTRAINT fk_cluster_filesystem_id FOREIGN KEY (filesystem_id) REFERENCES filesystem(id);


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY cluster
    DROP CONSTRAINT fk_cluster_filesystem_id;

ALTER TABLE ONLY cluster
    DROP COLUMN filesystem_id;

DROP SEQUENCE filesystem_table;

DROP TABLE IF EXISTS filesystem_properties;
DROP TABLE IF EXISTS filesystem;
