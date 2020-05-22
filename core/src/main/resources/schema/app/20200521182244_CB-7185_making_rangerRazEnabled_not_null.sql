-- // CB-7185 making rangerRazEnabled not null.
-- Migration SQL that makes the change goes here.


UPDATE cluster SET ranger_raz_enabled = FALSE WHERE ranger_raz_enabled is NULL;

ALTER TABLE cluster ALTER ranger_raz_enabled SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.


