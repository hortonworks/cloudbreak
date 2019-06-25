-- // CB-1859 set crn to stack
-- Migration SQL that makes the change goes here.
ALTER TABLE stack ADD COLUMN IF NOT EXISTS resourceCrn VARCHAR(255);

UPDATE stack
SET resourceCrn = CONCAT('crn:altus:cloudbreak:us-west-1:', (SELECT name from workspace WHERE id = SQ.workspace_id), ':stack:', stack.id)
FROM (SELECT *
	  FROM stack s
	  WHERE resourceCrn IS NULL) AS SQ
WHERE stack.workspace_id = SQ.workspace_id;

ALTER TABLE stack ALTER COLUMN resourceCrn SET NOT NULL;
ALTER TABLE stack ADD CONSTRAINT stack_crn_uq UNIQUE (resourceCrn);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE stack DROP COLUMN IF EXISTS resourceCrn;

