-- // BUG-97313 add unique constraint for account, name to proxyconfig
-- Migration SQL that makes the change goes here.

ALTER TABLE proxyconfig ADD CONSTRAINT uk_proxyconfig_account_name UNIQUE (account, name);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE proxyconfig DROP CONSTRAINT IF EXISTS uk_proxyconfig_account_name;
