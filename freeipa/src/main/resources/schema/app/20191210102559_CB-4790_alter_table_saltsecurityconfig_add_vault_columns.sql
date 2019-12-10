-- // CB-4790 alter table saltsecurityconfig add vault columns
-- Migration SQL that makes the change goes here.

ALTER TABLE saltsecurityconfig ADD COLUMN IF NOT EXISTS saltPasswordVault TEXT;
ALTER TABLE saltsecurityconfig ADD COLUMN IF NOT EXISTS saltSignPrivateKeyVault TEXT;
ALTER TABLE saltsecurityconfig ADD COLUMN IF NOT EXISTS saltBootPasswordVault TEXT;
ALTER TABLE saltsecurityconfig ADD COLUMN IF NOT EXISTS saltBootSignPrivateKeyVault TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE saltsecurityconfig DROP COLUMN IF  EXISTS saltPasswordVault;
ALTER TABLE saltsecurityconfig DROP COLUMN IF  EXISTS saltSignPrivateKeyVault;
ALTER TABLE saltsecurityconfig DROP COLUMN IF  EXISTS saltBootPasswordVault;
ALTER TABLE saltsecurityconfig DROP COLUMN IF  EXISTS saltBootSignPrivateKeyVault;
