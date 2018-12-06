-- // RMP-12926 Remove enableSecurityFlag
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS secure;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS secure boolean DEFAULT false;
UPDATE cluster SET secure = true WHERE kerberosconfig_id IS NOT NULL;