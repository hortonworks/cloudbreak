-- // CLOUD-66169 Cloudbreak Event Balloon and Event History messages are duplicated
-- Migration SQL that makes the change goes here.

delete from subscription where endpoint like '%:3000/notifications';

-- //@UNDO
-- SQL to undo the change goes here.


