-- // CB-18120 rename dde templates in case of google
-- Migration SQL that makes the change goes here.

UPDATE clustertemplate SET name = '7.2.14 - Data Discovery and Exploration for Google Cloud' WHERE status = 'DEFAULT' AND name = '7.2.14 - Data Discovery and Exploration for GCP';
UPDATE clustertemplate SET name = '7.2.15 - Data Discovery and Exploration for Google Cloud' WHERE status = 'DEFAULT' AND name = '7.2.15 - Data Discovery and Exploration for GCP';
UPDATE clustertemplate SET name = '7.2.16 - Data Discovery and Exploration for Google Cloud' WHERE status = 'DEFAULT' AND name = '7.2.16 - Data Discovery and Exploration for GCP';

-- //@UNDO
-- SQL to undo the change goes here.
