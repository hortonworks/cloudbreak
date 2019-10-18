-- // CB-4047 image catalog created field
-- Migration SQL that makes the change goes here.

ALTER TABLE imagecatalog ADD COLUMN created int8 NOT NULL DEFAULT (date_part('epoch'::text, now()) * 1000::double precision);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE imagecatalog DROP COLUMN created;

