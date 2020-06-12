-- // CB-7148 extend network entity with outbound internet traffic related fields
-- Migration SQL that makes the change goes here.

ALTER TABLE network ADD IF NOT EXISTS outboundInternetTraffic varchar(255);
ALTER TABLE network ALTER COLUMN outboundInternetTraffic SET DEFAULT 'ENABLED';
UPDATE network SET outboundInternetTraffic = 'ENABLED' WHERE outboundInternetTraffic IS NULL;
ALTER TABLE network ALTER COLUMN outboundInternetTraffic SET NOT NULL;

ALTER TABLE network ADD IF NOT EXISTS networkCidrs text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE network DROP COLUMN IF EXISTS outboundInternetTraffic;
ALTER TABLE network DROP COLUMN IF EXISTS networkCidrs;
