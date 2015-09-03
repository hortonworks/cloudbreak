-- // CLOUD-43504 load recipe from consul
-- Migration SQL that makes the change goes here.

ALTER TABLE recipe_plugins ALTER plugin TYPE text;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE recipe_plugins ALTER plugin TYPE character varying(255);