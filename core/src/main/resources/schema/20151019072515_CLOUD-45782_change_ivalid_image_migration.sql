-- // CLOUD-45782 change invalid image igration
-- Migration SQL that makes the change goes here.

UPDATE component SET componenttype = 'IMAGE' WHERE name = 'image';


-- //@UNDO
-- SQL to undo the change goes here.

