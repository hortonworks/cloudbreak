-- // BUG-107334 extend instance metadata with image information
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata DROP COLUMN IF EXISTS imageId;
ALTER TABLE instancemetadata ADD COLUMN IF NOT EXISTS image TEXT;

UPDATE instancemetadata im1 SET image = c.attributes FROM component c WHERE c.id =
    (SELECT c.id FROM instancemetadata im
                 INNER JOIN instancegroup ig ON ig.id = im.instancegroup_id
                 INNER JOIN component c ON c.stack_id = ig.stack_id
                 WHERE c.componenttype = 'IMAGE' AND im.id=im1.id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancemetadata ADD COLUMN IF NOT EXISTS imageId VARCHAR(40);
ALTER TABLE instancemetadata DROP COLUMN IF EXISTS image;
