-- // CB-13985 Add recipe foreign key for generated recipe table
-- Migration SQL that makes the change goes here.

ALTER TABLE generatedrecipe ADD COLUMN IF NOT EXISTS recipe_id bigint;
ALTER TABLE generatedrecipe ADD CONSTRAINT fk_generatedrecipe_recipe_id FOREIGN KEY (recipe_id) REFERENCES recipe(id) on delete cascade;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE generatedrecipe DROP CONSTRAINT IF EXISTS fk_generatedrecipe_recipe_id;
ALTER TABLE generatedrecipe DROP COLUMN IF EXISTS recipe_id;