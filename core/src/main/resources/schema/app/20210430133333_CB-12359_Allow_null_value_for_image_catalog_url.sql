-- // CB-12359 Allow null values for imagecatalog url
-- Migration SQL that makes the change goes here.

ALTER TABLE imagecatalog ALTER COLUMN url DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.