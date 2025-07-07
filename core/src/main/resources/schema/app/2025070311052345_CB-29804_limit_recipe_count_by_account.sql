-- // CB-29804 Add MAX count limit to recipes to avoid overloading CB services
-- Migration SQL that makes the change goes here.

ALTER TABLE recipe ADD COLUMN IF NOT EXISTS accountid varchar(255);
CREATE INDEX IF NOT EXISTS idx_recipe_accountid ON recipe (accountid, archived);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_recipe_accountid;
ALTER TABLE recipe DROP COLUMN IF EXISTS accountid;
