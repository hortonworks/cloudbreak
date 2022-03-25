-- // CB-15814 Add option
-- Migration SQL that makes the change goes here.


ALTER TABLE environment_network ADD IF NOT EXISTS loadbalancercreation varchar(255) DEFAULT 'ENABLED' NOT NULL;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS loadbalancercreation;
