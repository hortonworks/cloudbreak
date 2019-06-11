-- // CB-1807 add creator field
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS creator varchar(255) NOT NULL DEFAULT 'user';
ALTER TABLE credential ADD COLUMN IF NOT EXISTS creator varchar(255) NOT NULL DEFAULT 'user';
ALTER TABLE proxyconfig ADD COLUMN IF NOT EXISTS creator varchar(255) NOT NULL DEFAULT 'user';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS creator;
ALTER TABLE credential DROP COLUMN IF EXISTS creator;
ALTER TABLE proxyconfig DROP COLUMN IF EXISTS creator;
