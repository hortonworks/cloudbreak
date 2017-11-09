-- // CLOUD-91176 Uptime value resets after stopstart
-- Migration SQL that makes the change goes here.
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS uptime character varying(255);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE cluster DROP COLUMN IF EXISTS uptime;


