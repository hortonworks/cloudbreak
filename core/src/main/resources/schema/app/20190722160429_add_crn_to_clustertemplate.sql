-- // add crn to clustertemplate
-- Migration SQL that makes the change goes here.

ALTER TABLE clustertemplate ADD COLUMN IF NOT EXISTS resourceCrn VARCHAR(255);

UPDATE clustertemplate
SET resourceCrn = CONCAT('crn:altus:cloudbreak:us-west-1:', (SELECT name from workspace WHERE id = SQ.workspace_id), ':clustertemplate:', clustertemplate.id)
FROM (SELECT *
	  FROM clustertemplate rec
	  WHERE rec.resourceCrn IS NULL) AS SQ
WHERE clustertemplate.workspace_id = SQ.workspace_id;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE clustertemplate DROP COLUMN IF EXISTS resourceCrn;

