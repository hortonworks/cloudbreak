-- // CLOUD-87002 added user profile parameters
-- Migration SQL that makes the change goes here.


ALTER TABLE userprofile ADD COLUMN uiproperties TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE userprofile DROP COLUMN uiproperties;

