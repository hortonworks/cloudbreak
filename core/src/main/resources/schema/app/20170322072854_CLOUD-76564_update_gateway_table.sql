-- // CLOUD-76564 update gateway table
-- Migration SQL that makes the change goes here.

ALTER TABLE gateway ADD COLUMN gatewaytype varchar(255);
ALTER TABLE gateway ADD COLUMN ssotype varchar(255);

UPDATE gateway SET gatewaytype = 'INDIVIDUAL';
UPDATE gateway SET ssotype = 'NONE';

ALTER TABLE gateway ALTER COLUMN gatewaytype SET NOT NULL;
ALTER TABLE gateway ALTER COLUMN ssotype SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE gateway DROP COLUMN gatewaytype;
ALTER TABLE gateway DROP COLUMN ssotype;