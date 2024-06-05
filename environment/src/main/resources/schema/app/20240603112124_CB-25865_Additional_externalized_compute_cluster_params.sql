-- // CB-25865 - Additional externalized compute cluster parameters needs to be exposed
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS usepublicdnsforprivateaks bool NOT NULL DEFAULT false;
ALTER TABLE proxyconfig ADD COLUMN IF NOT EXISTS inboundproxycidr text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS usepublicdnsforprivateaks;
ALTER TABLE proxyconfig DROP COLUMN IF EXISTS inboundproxycidr;