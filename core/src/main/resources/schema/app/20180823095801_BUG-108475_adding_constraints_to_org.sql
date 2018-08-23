-- // BUG-108475 adding more constraints to orgs
-- Migration SQL that makes the change goes here.


UPDATE organization
SET deletiontimestamp = -1
WHERE deletiontimestamp IS NULL;

ALTER TABLE organization ALTER COLUMN name SET NOT NULL;
ALTER TABLE organization ALTER COLUMN deletiontimestamp SET NOT NULL;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE organization ALTER COLUMN name DROP NOT NULL;
ALTER TABLE organization ALTER COLUMN deletiontimestamp DROP NOT NULL;
