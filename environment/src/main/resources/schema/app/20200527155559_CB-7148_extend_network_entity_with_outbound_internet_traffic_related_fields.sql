-- // CB-7148 extend network entity with outbound internet traffic related fields
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_network ADD IF NOT EXISTS outboundInternetTraffic varchar(255);
ALTER TABLE environment_network ALTER COLUMN outboundInternetTraffic SET DEFAULT 'ENABLED';
UPDATE environment_network SET outboundInternetTraffic = 'ENABLED' WHERE outboundInternetTraffic IS NULL;
ALTER TABLE environment_network ALTER COLUMN outboundInternetTraffic SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS outboundInternetTraffic;
