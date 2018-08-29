-- // RMP-11057 Support for Recipe Parameters
-- Migration SQL that makes the change goes here.

CREATE SEQUENCE IF NOT EXISTS generatedrecipe_id_seq START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

CREATE TABLE  IF NOT EXISTS generatedrecipe (
  id bigint PRIMARY KEY DEFAULT nextval('generatedrecipe_id_seq'),
  hostgroup_id bigint NOT NULL,
  originalRecipe text,
  extendedRecipe text
);

CREATE TABLE hostgroup_generatedrecipe (
    hostgroup_id bigint NOT NULL,
    generatedrecipe_id bigint NOT NULL
);

ALTER TABLE ONLY hostgroup_generatedrecipe ADD CONSTRAINT hostgroup_generatedrecipe_pkey PRIMARY KEY (hostgroup_id, generatedrecipe_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS generatedrecipe;

DROP TABLE IF EXISTS hostgroup_generatedrecipe;

DROP SEQUENCE IF EXISTS generatedrecipe_id_seq;