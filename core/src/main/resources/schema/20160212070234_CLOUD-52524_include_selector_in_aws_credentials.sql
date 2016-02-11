-- // CLOUD-52524 include selector in aws credentials
-- Migration SQL that makes the change goes here.

ALTER TABLE credential ALTER COLUMN attributes SET DATA TYPE json USING attributes::json;
UPDATE credential SET attributes = json_build_object('roleArn', attributes->> 'roleArn', 'selector', 'role-based') WHERE cloudplatform = 'AWS';
ALTER TABLE credential ALTER COLUMN attributes SET DATA TYPE TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE credential ALTER COLUMN attributes SET DATA TYPE json USING attributes::json;
UPDATE credential SET attributes = json_build_object('roleArn', attributes->> 'roleArn') WHERE cloudplatform = 'AWS';
ALTER TABLE credential ALTER COLUMN attributes SET DATA TYPE TEXT;