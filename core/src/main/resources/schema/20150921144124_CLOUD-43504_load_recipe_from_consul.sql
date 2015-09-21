-- // CLOUD-43504 load recipe from consul
-- Migration SQL that makes the change goes here.

ALTER TABLE ONLY recipe_plugins DROP CONSTRAINT recipe_plugins_pkey;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY recipe_plugins ADD CONSTRAINT recipe_plugins_pkey PRIMARY KEY (recipe_id, plugin);