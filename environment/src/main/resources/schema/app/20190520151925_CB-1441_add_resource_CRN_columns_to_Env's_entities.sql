-- // CB-1441 add resource CRN columns to Env's entities
-- Migration SQL that makes the change goes here.
ALTER TABLE credential ADD COLUMN IF NOT EXISTS resourcecrn varchar(255) NOT NULL;
CREATE INDEX IF NOT EXISTS credential_accountid_resourcecrn_idx ON credential (accountid, resourcecrn);

ALTER TABLE proxyconfig ADD COLUMN IF NOT EXISTS resourcecrn varchar(255) NOT NULL;
CREATE INDEX IF NOT EXISTS proxyconfig_accountid_resourcecrn_idx ON proxyconfig  (accountid, resourcecrn);

ALTER TABLE environment ADD COLUMN IF NOT EXISTS resourcecrn varchar(255) NOT NULL;
CREATE INDEX IF NOT EXISTS environment_accountid_resourcecrn_idx ON environment  (accountid, resourcecrn);

ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS resourcecrn varchar(255) NOT NULL;
CREATE INDEX IF NOT EXISTS environment_network_accountid_resourcecrn_idx ON environment_network  (accountid, resourcecrn);


-- //@UNDO
-- SQL to undo the change goes here.
DROP INDEX IF EXISTS credential_accountid_resourcecrn_idx;
ALTER TABLE credential DROP COLUMN IF EXISTS resourcecrn;

DROP INDEX IF EXISTS proxyconfig_accountid_resourcecrn_idx;
ALTER TABLE proxyconfig DROP COLUMN IF EXISTS resourcecrn;

DROP INDEX IF EXISTS environment_accountid_resourcecrn_idx;
ALTER TABLE environment DROP COLUMN IF EXISTS resourcecrn;

DROP INDEX IF EXISTS environment_network_accountid_resourcecrn_idx;
ALTER TABLE environment_network DROP COLUMN IF EXISTS resourcecrn;


