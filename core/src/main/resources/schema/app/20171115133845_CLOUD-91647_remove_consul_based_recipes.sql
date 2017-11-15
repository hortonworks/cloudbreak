-- // CLOUD-91647 remove consul based recipes
-- Migration SQL that makes the change goes here.

DROP TABLE IF EXISTS plugin;

DROP SEQUENCE IF EXISTS plugin_id_seq;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE SEQUENCE plugin_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE plugin (
    id              bigint NOT NULL,
    recipe_id       bigint NOT NULL,
    content         TEXT NOT NULL
);