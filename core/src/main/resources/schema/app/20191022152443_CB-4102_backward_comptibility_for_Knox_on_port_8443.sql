-- // CB-4102 backward comptibility for Knox on port 8443
-- Migration SQL that makes the change goes here.
ALTER TABLE gateway ADD COLUMN gatewayport INTEGER default 8443;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE gateway DROP COLUMN gatewayport;

