-- // CLOUD-61094_remove_service_componenttype
-- Migration SQL that makes the change goes here.

DELETE FROM component WHERE componenttype='SERVICE';

-- //@UNDO
-- SQL to undo the change goes here.
