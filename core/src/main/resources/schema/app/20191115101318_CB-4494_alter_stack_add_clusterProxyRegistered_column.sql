-- // CB-4494 alter stack add clusterProxyRegistered column
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS clusterproxyregistered BOOLEAN;

UPDATE stack SET clusterproxyregistered = FALSE WHERE clusterproxyregistered IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS clusterproxyregistered;
