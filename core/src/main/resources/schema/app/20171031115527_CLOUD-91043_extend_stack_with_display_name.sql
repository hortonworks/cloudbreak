-- // CLOUD-91043 extend stack with display name
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS displayname VARCHAR(255);

UPDATE stack SET displayname = stack.name;

-- //@UNDO
-- SQL to undo the change goes here.
