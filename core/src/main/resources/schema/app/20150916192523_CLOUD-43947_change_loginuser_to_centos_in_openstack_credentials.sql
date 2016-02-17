-- // CLOUD-43947: change loginuser to centos in openstack credentials
-- Migration SQL that makes the change goes here.

UPDATE credential SET loginusername='centos' where dtype='OpenStackCredential';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE credential SET loginusername='ec2-user' where dtype='OpenStackCredential';
