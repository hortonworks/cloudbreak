-- // CB-26500 Change type of encryptionkey columns in the stackencryption table
-- Migration SQL that makes the change goes here.

ALTER TABLE stackencryption ALTER COLUMN encryptionkeyluks TYPE text;
ALTER TABLE stackencryption ALTER COLUMN encryptionkeycloudsecretmanager TYPE text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stackencryption ALTER COLUMN encryptionkeyluks TYPE varchar(255);
ALTER TABLE stackencryption ALTER COLUMN encryptionkeycloudsecretmanager TYPE varchar(255);
