-- // CB-28693 Add environmentType attribute for Environment
-- Migration SQL that makes the change goes here.
ALTER TABLE environment ADD COLUMN IF NOT EXISTS environmenttype varchar(255) DEFAULT 'PUBLIC_CLOUD';

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment DROP COLUMN IF EXISTS environmentType;


