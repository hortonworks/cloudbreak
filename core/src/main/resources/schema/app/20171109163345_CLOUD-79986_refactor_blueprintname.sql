-- // CLOUD-79986 refactor blueprintname
-- Migration SQL that makes the change goes here.
ALTER TABLE blueprint RENAME COLUMN blueprintname TO ambariname;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE blueprint RENAME COLUMN ambariname TO blueprintname;


