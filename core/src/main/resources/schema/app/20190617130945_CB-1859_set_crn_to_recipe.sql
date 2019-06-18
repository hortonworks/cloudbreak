-- // CB-1859 change recipe id to crn
-- Migration SQL that makes the change goes here.
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS crn VARCHAR(255);
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS creator VARCHAR(255);

UPDATE recipe
SET crn = CONCAT('crn:altus:cloudbreak:us-west-1:', (SELECT name from workspace WHERE id = SQ.workspace_id), ':recipe:', recipe.id)
FROM (SELECT *
	  FROM recipe rec
	  WHERE rec.crn IS NULL) AS SQ
WHERE recipe.workspace_id = SQ.workspace_id;

ALTER TABLE recipe ALTER COLUMN crn SET NOT NULL;
ALTER TABLE recipe ADD CONSTRAINT recipe_crn_uq UNIQUE (crn);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE recipe DROP COLUMN IF EXISTS crn;
ALTER TABLE recipe DROP COLUMN IF EXISTS creator;
