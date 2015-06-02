-- // CLOUD-399_store_cert_dir_in_stack
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN certdir VARCHAR (255);


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN certdir;
