-- // network resource change
-- Migration SQL that makes the change goes here.

ALTER TABLE network ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE network ALTER COLUMN account DROP NOT NULL;
ALTER TABLE network DROP CONSTRAINT IF EXISTS uk_network_account_name;


-- //@UNDO
-- SQL to undo the change goes here.


