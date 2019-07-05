-- // DISTX-197 Implement Logging/WXM request support on Env side
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS queue varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS queue;