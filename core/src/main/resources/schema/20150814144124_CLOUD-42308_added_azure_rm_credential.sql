-- // CLOUD-42308 added azure rm credential
-- Migration SQL that makes the change goes here.

ALTER TABLE credential ADD COLUMN tenantid CHARACTER VARYING (255);
ALTER TABLE credential ADD COLUMN acceskey CHARACTER VARYING (255);
ALTER TABLE credential ADD COLUMN secretkey CHARACTER VARYING (255);


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE credential DROP COLUMN acceskey;
ALTER TABLE credential DROP COLUMN secretkey;
ALTER TABLE credential DROP COLUMN tenantid;