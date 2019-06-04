-- // CB-1606 ensure unique, not terminated stack in environment
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ALTER COLUMN terminated SET DEFAULT -1;

UPDATE stack SET terminated = -1 WHERE terminated IS NULL;

ALTER TABLE stack ALTER COLUMN terminated SET NOT NULL;

ALTER TABLE stack DROP CONSTRAINT IF EXISTS acc_env_term_uniq;

ALTER TABLE stack ADD CONSTRAINT acc_env_term_uniq UNIQUE (accountid, environment, terminated);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP CONSTRAINT IF EXISTS acc_env_term_uniq;

ALTER TABLE stack ALTER COLUMN terminated DROP NOT NULL;

UPDATE stack SET terminated = NULL WHERE terminated = -1;

ALTER TABLE stack ALTER COLUMN terminated DROP DEFAULT;
