-- // CB-6559 extend environment security group cidr column
-- Migration SQL that makes the change goes here.
ALTER TABLE IF EXISTS environment ALTER COLUMN cidr SET DATA TYPE VARCHAR(255);


-- //@UNDO
-- SQL to undo the change goes here.


