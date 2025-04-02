-- // CB-29105 Add remoteenvironmentcrn attribute for Environment
-- Migration SQL that makes the change goes here.
ALTER TABLE environment ADD COLUMN IF NOT EXISTS remoteenvironmentcrn varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment DROP COLUMN IF EXISTS remoteenvironmentcrn;


