-- // CB-13668 Add SKU field to load balancer table
-- Migration SQL that makes the change goes here.

ALTER TABLE loadbalancer ADD COLUMN IF NOT EXISTS sku VARCHAR(20);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE loadbalancer DROP COLUMN IF EXISTS sku;
