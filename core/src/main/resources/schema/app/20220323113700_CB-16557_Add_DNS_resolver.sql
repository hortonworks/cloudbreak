-- // CB-16557 Check if all unbound related node list config are removed without using salt
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS domainDnsResolver VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS domainDnsResolver;
