-- // Add orchestratorType to constrainttemplate
-- Migration SQL that makes the change goes here.
ALTER TABLE constrainttemplate
    ADD COLUMN orchestratortype CHARACTER VARYING (255) NOT NULL DEFAULT 'MARATHON';
ALTER TABLE constrainttemplate
    ALTER COLUMN orchestratortype DROP DEFAULT;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE constrainttemplate DROP COLUMN orchestratortype;

