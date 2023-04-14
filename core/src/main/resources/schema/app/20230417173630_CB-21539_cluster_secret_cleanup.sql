-- // CB-21539 cluster secret cleanup
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS clustermanagermgmtpassword TEXT;
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS clustermanagermgmtuser TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE CLUSTER SET dpambariuser = clustermanagermgmtuser WHERE clustermanagermgmtuser IS NOT NULL;
UPDATE CLUSTER SET dpclustermanageruser = clustermanagermgmtuser WHERE clustermanagermgmtuser IS NOT NULL;
UPDATE CLUSTER SET dpambaripassword = clustermanagermgmtpassword WHERE clustermanagermgmtpassword IS NOT NULL;
UPDATE CLUSTER SET dpclustermanagerpassword = clustermanagermgmtpassword WHERE clustermanagermgmtpassword IS NOT NULL;
ALTER TABLE cluster DROP COLUMN IF EXISTS clustermanagermgmtpassword;
ALTER TABLE cluster DROP COLUMN IF EXISTS clustermanagermgmtuser;

