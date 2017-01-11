-- // CLOUD-72055 use cloudbreak user by default
-- Migration SQL that makes the change goes here.

UPDATE credential SET loginusername = 'cloudbreak' WHERE cloudplatform = 'AWS';

-- //@UNDO
-- SQL to undo the change goes here.


UPDATE credential SET loginusername = 'ec2-user' WHERE cloudplatform = 'AWS';
