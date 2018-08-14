-- // BUG-108475_proxyconfig_v3
-- Migration SQL that makes the change goes here.

ALTER TABLE proxyconfig ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE proxyconfig ALTER COLUMN account DROP NOT NULL;
ALTER TABLE proxyconfig DROP CONSTRAINT IF EXISTS uk_proxyconfig_account_name;

-- //@UNDO
-- SQL to undo the change goes here.
