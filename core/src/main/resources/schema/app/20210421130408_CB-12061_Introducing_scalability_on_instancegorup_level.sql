-- // CB-12061 Introducing scalability on instancegorup level
-- Migration SQL that makes the change goes here.

ALTER TABLE instancegroup ADD COLUMN IF NOT EXISTS scalabilityoption CHARACTER VARYING (255) ;
UPDATE instancegroup SET scalabilityoption='ALLOWED' WHERE scalabilityoption IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancegroup DROP COLUMN IF EXISTS scalabilityoption;


