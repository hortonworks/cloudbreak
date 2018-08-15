-- // BUG-108475 image catalog auth
-- Migration SQL that makes the change goes here.

ALTER TABLE imagecatalog ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE imagecatalog ALTER COLUMN account DROP NOT NULL;
ALTER TABLE imagecatalog DROP CONSTRAINT IF EXISTS uk_imagecatalog_account_name;

-- //@UNDO
-- SQL to undo the change goes here.


