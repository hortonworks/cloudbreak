-- // CLOUD-57201 add nginx port to stack
-- Migration SQL that makes the change goes here.

ALTER TABLE stack
    ADD COLUMN gatewayport INTEGER NOT NULL DEFAULT 443;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN gatewayport;
