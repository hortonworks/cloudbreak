-- // CB-29301 Freeipa architecture on stack and image
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS architecture varchar(255) DEFAULT 'X86_64';
ALTER TABLE image ADD COLUMN IF NOT EXISTS architecture varchar(255) DEFAULT 'X86_64';
ALTER TABLE image_history ADD COLUMN IF NOT EXISTS architecture varchar(255) DEFAULT 'X86_64';

UPDATE stack SET architecture = 'X86_64' WHERE architecture IS NULL;
UPDATE image SET architecture = 'X86_64' WHERE architecture IS NULL;
UPDATE image_history SET architecture = 'X86_64' WHERE architecture IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.