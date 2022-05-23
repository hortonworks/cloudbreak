-- // CB-16699 Bring your own DNS zone - Rename endpoint body field, step 2
-- Migration SQL that makes the change goes here.
CREATE SEQUENCE IF NOT EXISTS stack_recipes_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS stack_recipes
(
    id                  BIGINT PRIMARY KEY NOT NULL DEFAULT nextval('stack_recipes_id_seq'),
    stack_id            BIGINT NOT NULL,
    recipe              CHARACTER VARYING (255),
    CONSTRAINT fk_env_stackrecipes_stackid FOREIGN KEY (stack_id) REFERENCES stack(id)
);


-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS stack_recipes;

DROP SEQUENCE IF EXISTS stack_recipes_id_seq;