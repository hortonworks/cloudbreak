-- // BUG-108475
-- Migration SQL that makes the change goes here.

ALTER TABLE recipe DROP CONSTRAINT IF EXISTS uk_recipe_account_name;

ALTER TABLE stack ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE stack ALTER COLUMN account DROP NOT NULL;
ALTER TABLE stack DROP CONSTRAINT IF EXISTS uk_stack_account_name;

-- //@UNDO
-- SQL to undo the change goes here.


