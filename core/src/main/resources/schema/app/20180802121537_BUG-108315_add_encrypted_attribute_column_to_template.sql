-- // BUG-108315 add encrypted attribute column to template
-- Migration SQL that makes the change goes here.
ALTER TABLE template ADD COLUMN secretAttributes TEXT;
UPDATE template SET secretAttributes='{}';

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE template DROP COLUMN IF EXISTS secretAttributes;


