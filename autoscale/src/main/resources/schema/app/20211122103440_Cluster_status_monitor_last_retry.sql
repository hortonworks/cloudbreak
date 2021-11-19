-- // CB-12843 Make sure we have PKs on every CB table
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN lastretried BIGINT DEFAULT 0;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS lastretried;
