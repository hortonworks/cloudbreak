-- // CB-24646 Salt masters have different private keys in HA applications
-- Migration SQL that makes the change goes here.

ALTER TABLE saltsecurityconfig ADD COLUMN IF NOT EXISTS saltmasterpublickey TEXT;
ALTER TABLE saltsecurityconfig ADD COLUMN IF NOT EXISTS saltmasterprivatekey TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE saltsecurityconfig DROP COLUMN IF EXISTS saltmasterpublickey;
ALTER TABLE saltsecurityconfig DROP COLUMN IF EXISTS saltmasterprivatekey;
