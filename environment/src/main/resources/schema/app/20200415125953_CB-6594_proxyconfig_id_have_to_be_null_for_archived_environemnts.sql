-- // CB-6594 proxyconfig id have to be null for archived environemnts
-- Migration SQL that makes the change goes here.

UPDATE environment SET proxyconfig_id = null WHERE archived = true AND proxyconfig_id IS NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.
