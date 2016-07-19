-- // CLOUD-62544 mail to field added to cluster
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN emailto character varying(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN emailto;

