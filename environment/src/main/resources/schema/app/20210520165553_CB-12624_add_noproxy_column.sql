-- // CB-12646 add noproxyhosts field to store no_proxy list
-- Migration SQL that makes the change goes here.

ALTER TABLE proxyconfig ADD COLUMN IF NOT EXISTS noproxyhosts varchar(4000);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE proxyconfig DROP COLUMN IF EXISTS noproxyhosts;
