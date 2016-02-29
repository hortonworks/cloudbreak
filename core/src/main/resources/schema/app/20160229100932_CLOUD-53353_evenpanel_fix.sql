-- // CLOUD-53353 evenpanel fix
-- Migration SQL that makes the change goes here.

ALTER TABLE cloudbreakevent ADD COLUMN clusterid BIGINT;
ALTER TABLE cloudbreakevent ADD COLUMN clustername CHARACTER VARYING (255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cloudbreakevent DROP COLUMN clusterid;
ALTER TABLE cloudbreakevent DROP COLUMN clustername;

