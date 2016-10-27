-- // CLOUD-64886_securityconfig_gets_salt_passwords
-- Migration SQL that makes the change goes here.

ALTER TABLE securityconfig RENAME COLUMN temporarysshpublickey TO cloudbreaksshpublickey;
ALTER TABLE securityconfig RENAME COLUMN temporarysshprivatekey TO cloudbreaksshprivatekey;

ALTER TABLE securityconfig ADD COLUMN saltpassword varchar(255);
ALTER TABLE securityconfig ADD COLUMN saltbootpassword varchar(255);

UPDATE securityconfig SET saltpassword = (SELECT saltpassword FROM stack WHERE stack.id = securityconfig.stack_id);

ALTER TABLE stack DROP COLUMN saltpassword;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack ADD COLUMN saltpassword varchar(255);

UPDATE stack SET saltpassword = (SELECT saltpassword FROM securityconfig WHERE securityconfig.stack_id = stack.id);

ALTER TABLE securityconfig DROP COLUMN saltpassword;
ALTER TABLE securityconfig DROP COLUMN saltbootpassword;

ALTER TABLE securityconfig RENAME COLUMN cloudbreaksshpublickey TO temporarysshpublickey;
ALTER TABLE securityconfig RENAME COLUMN cloudbreaksshprivatekey TO temporarysshprivatekey;