-- // LOUD-50456 update_unregistered_instances_to_registered
-- Migration SQL that makes the change goes here.

UPDATE instancemetadata set instancestatus = 'REGISTERED' WHERE instancestatus = 'UNREGISTERED';

-- //@UNDO
-- SQL to undo the change goes here.


