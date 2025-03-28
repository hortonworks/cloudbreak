-- // CB-28453 SDX cluster template update with proper tags
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET tags='{"shared_services_ready":true}' WHERE name LIKE '%SDX%' AND status='DEFAULT';

-- //@UNDO
-- SQL to undo the change goes here.


