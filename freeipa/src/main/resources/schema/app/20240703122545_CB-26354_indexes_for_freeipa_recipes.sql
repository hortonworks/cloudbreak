-- // CB-26354 indexes for recipes
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_stack_recipes_stack_id ON stack_recipes (stack_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_stack_recipes_stack_id;
