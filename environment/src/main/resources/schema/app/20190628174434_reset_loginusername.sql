-- // reset loginusername
-- Migration SQL that makes the change goes here.

alter table environment_authentication alter column loginusername drop not null;

-- //@UNDO
-- SQL to undo the change goes here.

alter table environment_authentication alter column loginusername SET NOT NULL;

