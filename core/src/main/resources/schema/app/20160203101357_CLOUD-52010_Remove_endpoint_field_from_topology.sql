-- // CLOUD-52010 Remove endpoint field from topology
-- Migration SQL that makes the change goes here.

ALTER TABLE topology DROP COLUMN endpoint;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE topology ADD endpoint character varying(255);

