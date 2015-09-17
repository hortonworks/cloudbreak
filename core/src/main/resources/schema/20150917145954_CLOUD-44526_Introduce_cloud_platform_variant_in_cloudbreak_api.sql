-- // CLOUD-44526 Introduce cloud platform variant in cloudbreak api
-- Migration SQL that makes the change goes here.
ALTER TABLE stack ADD COLUMN platformvariant text;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE stack DROP COLUMN platformvariant;

