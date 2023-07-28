-- // CB-22614 Gateway cert rotation
-- Migration SQL that makes the change goes here.

ALTER TABLE gateway ADD COLUMN IF NOT EXISTS signcertsecret TEXT;
ALTER TABLE gateway ADD COLUMN IF NOT EXISTS signpubsecret TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE gateway DROP COLUMN IF EXISTS signcertsecret;
ALTER TABLE gateway DROP COLUMN IF EXISTS signpubsecret;
