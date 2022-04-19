-- // CB-16699 Bring your own DNS zone - prepare private DNS zone endpoint name multiple private DNS zones
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_network RENAME COLUMN privatednszoneid TO databaseprivatednszoneid;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network RENAME COLUMN databaseprivatednszoneid TO privatednszoneid;