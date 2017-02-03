-- // CLOUD-65515 new Azure connector
-- Migration SQL that makes the change goes here.

UPDATE account_preferences SET platforms = replace(platforms, 'AZURE_RM', 'AZURE');
UPDATE credential SET cloudplatform='AZURE' WHERE cloudplatform='AZURE_RM';
UPDATE network SET cloudplatform='AZURE' WHERE cloudplatform='AZURE_RM';
UPDATE stack SET cloudplatform='AZURE' WHERE cloudplatform='AZURE_RM';
UPDATE stack SET platformvariant='AZURE' WHERE platformvariant='AZURE_RM';
UPDATE securitygroup SET cloudplatform='AZURE' WHERE cloudplatform='AZURE_RM';
UPDATE template SET cloudplatform='AZURE' WHERE cloudplatform='AZURE_RM';
UPDATE topology SET cloudplatform='AZURE' WHERE cloudplatform='AZURE_RM';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE account_preferences SET platforms = replace(platforms, 'AZURE', 'AZURE_RM');
UPDATE credential SET cloudplatform='AZURE_RM' WHERE cloudplatform='AZURE';
UPDATE network SET cloudplatform='AZURE_RM' WHERE cloudplatform='AZURE';
UPDATE stack SET cloudplatform='AZURE_RM' WHERE cloudplatform='AZURE';
UPDATE stack SET platformvariant='AZURE_RM' WHERE platformvariant='AZURE';
UPDATE securitygroup SET cloudplatform='AZURE_RM' WHERE cloudplatform='AZURE';
UPDATE template SET cloudplatform='AZURE_RM' WHERE cloudplatform='AZURE';
UPDATE topology SET cloudplatform='AZURE_RM' WHERE cloudplatform='AZURE';