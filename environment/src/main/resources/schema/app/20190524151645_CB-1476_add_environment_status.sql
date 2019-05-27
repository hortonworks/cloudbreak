-- // CB-1476 adding environment status
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS status varchar(255) NOT NULL DEFAULT 'AVAILABLE';
CREATE INDEX IF NOT EXISTS environment_status_accountid_idx ON environment(status, accountid);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_network DROP COLUMN IF EXISTS status;


