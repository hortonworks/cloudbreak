-- // CB-14758 update AWS security group resources to have instance group name reference based on resource name
-- Migration SQL that makes the change goes here.

UPDATE resource SET instancegroup = lower(substring(resourcename from '[a-zA-Z0-9]*$')) where resourcetype = 'AWS_SECURITY_GROUP' AND instancegroup is NULL;

-- //@UNDO
-- SQL to undo the change goes here.


