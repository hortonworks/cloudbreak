-- // CLOUD-69005 cleanup recipe_database structure
-- Migration SQL that makes the change goes here.


DROP TABLE recipe_keyvalues;


-- uri
ALTER TABLE recipe ADD COLUMN uri varchar(255);

-- recipeType
ALTER TABLE recipe ADD COLUMN recipeType varchar(31);

UPDATE recipe SET recipeType = 'LEGACY' WHERE recipeType IS NULL;

ALTER TABLE recipe ALTER COLUMN recipeType SET NOT NULL;


-- content
ALTER TABLE recipe ADD COLUMN content TEXT;

UPDATE recipe SET content = 'LEGACY' WHERE content IS NULL;

ALTER TABLE recipe ALTER COLUMN content SET NOT NULL;


-- //@UNDO
-- SQL to undo the change goes here.


CREATE TABLE recipe_keyvalues (
    recipe_id bigint NOT NULL,
    value text,
    key character varying(255) NOT NULL
);

ALTER TABLE ONLY recipe_keyvalues
    ADD CONSTRAINT recipe_keyvalues_pkey PRIMARY KEY (recipe_id, key);

ALTER TABLE ONLY recipe_keyvalues
    ADD CONSTRAINT fk_recipe_keyvalues_recipe_id FOREIGN KEY (recipe_id) REFERENCES recipe(id);


DELETE FROM recipe WHERE recipeType NOT IN ('LEGACY', 'MIGRATED' );

ALTER TABLE recipe DROP COLUMN uri;

ALTER TABLE recipe DROP COLUMN recipeType;

ALTER TABLE recipe DROP COLUMN content;