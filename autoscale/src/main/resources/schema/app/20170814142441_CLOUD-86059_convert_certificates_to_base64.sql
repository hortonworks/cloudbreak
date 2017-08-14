-- // CLOUD-86059 convert certificates to base64
-- Migration SQL that makes the change goes here.

UPDATE securityconfig SET clientkey = encode(clientkey, 'base64')::bytea;
UPDATE securityconfig SET clientcert = encode(clientcert, 'base64')::bytea;
UPDATE securityconfig SET servercert = encode(servercert, 'base64')::bytea;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE securityconfig SET clientkey = decode(convert_from(clientkey, 'utf-8'), 'base64');
UPDATE securityconfig SET clientcert = decode(convert_from(clientcert, 'utf-8'), 'base64');
UPDATE securityconfig SET servercert = decode(convert_from(servercert, 'utf-8'), 'base64');
