-- // CB-3410 add fqdn to Cluster
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN fqdn varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE cluster DROP COLUMN fqdn;

