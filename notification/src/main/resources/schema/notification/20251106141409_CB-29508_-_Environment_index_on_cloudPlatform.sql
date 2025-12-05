-- // CB-29508 - Environment index on cloudPlatform
-- Migration SQL that makes the change goes here.
CREATE INDEX IF NOT EXISTS environment_cloudplatform_resourcecrn_idx
    ON environment (cloudplatform, resourcecrn)
    WHERE archived = false;

-- //@UNDO
-- SQL to undo the change goes here.
DROP INDEX IF EXISTS environment_cloudplatform_resourcecrn_idx;