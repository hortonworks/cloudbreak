-- // CLOUD-87001 separate ssh keys from credentials
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN loginusername text;
ALTER TABLE stack ADD COLUMN publickey text;

UPDATE stack SET publickey = credential.publickey, loginusername = credential.loginusername FROM credential WHERE stack.credential_id = credential.id AND stack.publickey ISNULL AND credential.publickey NOTNULL ;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS loginusername;
ALTER TABLE stack DROP COLUMN IF EXISTS publickey;