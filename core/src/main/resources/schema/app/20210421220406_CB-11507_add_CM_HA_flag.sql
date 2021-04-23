-- // CB-11507 add CM HA flag
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN cm_ha_enabled BOOLEAN DEFAULT FALSE NOT NULL;


-- //@UNDO
-- SQL to undo the change goes here.


ALTER TABLE cluster DROP COLUMN cm_ha_enabled;