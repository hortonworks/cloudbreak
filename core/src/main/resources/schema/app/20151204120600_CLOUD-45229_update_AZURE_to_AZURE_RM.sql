-- // CLOUD-45229 update AZURE to AZURE_RM
-- Migration SQL that makes the change goes here.

UPDATE template SET cloudplatform='AZURE_RM' WHERE cloudplatform='AZURE';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE template SET cloudplatform='AZURE' WHERE cloudplatform='AZURE_RM';