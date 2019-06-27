-- // CB-1859 set crn to image catalog
-- Migration SQL that makes the change goes here.
ALTER TABLE imagecatalog ADD COLUMN IF NOT EXISTS crn VARCHAR(255);
ALTER TABLE imagecatalog ADD COLUMN IF NOT EXISTS creator VARCHAR(255);

UPDATE imagecatalog
SET crn = CONCAT('crn:altus:cloudbreak:us-west-1:', (SELECT name from workspace WHERE id = SQ.workspace_id), ':imageCatalog:', imagecatalog.id)
FROM (SELECT *
	  FROM imagecatalog imgc
	  WHERE crn IS NULL) AS SQ
WHERE imagecatalog.workspace_id = SQ.workspace_id;

ALTER TABLE imagecatalog ALTER COLUMN crn SET NOT NULL;
ALTER TABLE imagecatalog ADD CONSTRAINT imgc_crn_uq UNIQUE (crn);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE imagecatalog DROP COLUMN IF EXISTS crn;
ALTER TABLE imagecatalog DROP COLUMN IF EXISTS creator;
