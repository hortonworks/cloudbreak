-- // CLOUD-42849 Ability to restart pre-install/post-install phases
-- Migration SQL that makes the change goes here.

ALTER TABLE cloudbreakevent ADD COLUMN clusterstatus CHARACTER VARYING (255);


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cloudbreakevent DROP COLUMN clusterstatus;