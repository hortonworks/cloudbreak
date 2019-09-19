-- // CB-3573 remove OpDB custom blueprint (no matching cluster template)
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name LIKE 'CDP%Operational Database%ustom%';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name LIKE 'CDP%Operational Database%ustom%';
