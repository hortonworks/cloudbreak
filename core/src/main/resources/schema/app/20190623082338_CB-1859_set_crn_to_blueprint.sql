-- // CB-1859 set crn to blueprint
-- Migration SQL that makes the change goes here.
ALTER TABLE blueprint ADD COLUMN IF NOT EXISTS crn VARCHAR(255);
ALTER TABLE blueprint ADD COLUMN IF NOT EXISTS creator VARCHAR(255);

 UPDATE blueprint
SET crn = CONCAT('crn:altus:cloudbreak:us-west-1:', (SELECT name from workspace WHERE id = SQ.workspace_id), ':blueprint:', blueprint.id)
FROM (SELECT *
	  FROM blueprint bp
	  WHERE crn IS NULL) AS SQ
WHERE blueprint.workspace_id = SQ.workspace_id;

ALTER TABLE blueprint ALTER COLUMN crn SET NOT NULL;
ALTER TABLE blueprint ADD CONSTRAINT bp_crn_uq UNIQUE (crn);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE blueprint DROP COLUMN IF EXISTS crn;
ALTER TABLE blueprint DROP COLUMN IF EXISTS creator;

