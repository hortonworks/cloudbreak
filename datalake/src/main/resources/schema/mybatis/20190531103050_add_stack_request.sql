-- // CB-9 SDX Management can post a valid cluster request to Cloudbreak
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS stackrequest TEXT;
ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS stackrequestToCloudbreak TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS stackrequest;
ALTER TABLE sdxcluster DROP COLUMN IF EXISTS stackrequestToCloudbreak;