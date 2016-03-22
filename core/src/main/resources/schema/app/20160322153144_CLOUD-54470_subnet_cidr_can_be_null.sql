-- // CLOUD-54470 subnet cidr can be null
-- Migration SQL that makes the change goes here.

ALTER TABLE network ALTER COLUMN subnetcidr DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE network ALTER COLUMN subnetcidr SET NOT NULL;
