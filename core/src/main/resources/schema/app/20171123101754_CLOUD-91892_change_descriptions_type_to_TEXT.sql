-- // CLOUD-91892 change descriptions type to TEXT
-- Migration SQL that makes the change goes here.
ALTER TABLE recipe ALTER description TYPE TEXT;

-- //@UNDO
-- SQL to undo the change goes here.
UPDATE recipe SET description=substring(description from 0 for 255);

ALTER TABLE recipe ALTER description TYPE character varying(255);

