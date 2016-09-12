-- // CLOUD-57217_add_stalt_password_to_stack
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN saltpassword varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN saltpassword;