-- // CB-14390 Refine datalake/datahub image operation names
-- Migration SQL that makes the change goes here.

UPDATE customimage SET imageType = 'RUNTIME'
WHERE imageType = 'DATAHUB' OR imageType = 'DATALAKE';

-- //@UNDO
-- SQL to undo the change goes here.
UPDATE customimage SET imageType = 'DATAHUB'
WHERE imageType = 'RUNTIME';