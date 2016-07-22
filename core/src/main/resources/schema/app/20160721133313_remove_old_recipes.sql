-- // remove old recipes
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS recipe_plugins DROP CONSTRAINT IF EXISTS fk_recipe_plugins_recipe_id;
DELETE FROM recipe_keyvalues k WHERE k.recipe_id IN (SELECT rp.recipe_id FROM recipe_plugins rp);
DELETE FROM recipe r WHERE r.id IN (SELECT rp.recipe_id FROM recipe_plugins rp);
DROP TABLE recipe_plugins;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE recipe_plugins
(
   recipe_id        bigint,
   execution_type   CHARACTER VARYING (255),
   plugin           text
);

ALTER TABLE recipe_plugins ADD CONSTRAINT fk_recipe_plugins_recipe_id FOREIGN KEY (recipe_id) REFERENCES recipe (id);
