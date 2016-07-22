-- // CLOUD-123 add plugin table
-- Migration SQL that makes the change goes here.

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

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE plugin;

DROP SEQUENCE  IF EXISTS  plugin_id_seq;