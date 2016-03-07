-- // CLOUD-53847 update azure disk type to Standard_LRS
-- Migration SQL that makes the change goes here.

UPDATE template SET volumetype = 'Standard_LRS' WHERE cloudplatform = 'AZURE_RM' AND volumetype = 'HDD';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE template SET volumetype = 'HDD' WHERE cloudplatform = 'AZURE_RM' AND volumetype = 'Standard_LRS';


