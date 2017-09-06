-- // CLOUD-84181 Structured event tables
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE structuredevent_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;

CREATE TABLE structuredevent
(
   id                     bigint NOT NULL DEFAULT nextval('structuredevent_id_seq'),
   structuredeventjson           TEXT
);

ALTER TABLE ONLY structuredevent
ADD CONSTRAINT structuredevent_pkey PRIMARY KEY (id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS structuredevent
DROP SEQUENCE IF EXISTS structuredevent_id_seq;
