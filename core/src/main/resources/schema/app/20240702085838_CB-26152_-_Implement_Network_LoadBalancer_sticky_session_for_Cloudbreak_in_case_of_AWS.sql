-- // CB-26152 - Implement Network LoadBalancer sticky session for Cloudbreak in case of AWS
-- Migration SQL that makes the change goes here.
ALTER TABLE targetgroup
ADD COLUMN IF NOT EXISTS "use_sticky_session" BOOLEAN
DEFAULT FALSE;

COMMENT ON COLUMN targetgroup."use_sticky_session" IS 'Whether the sticky session attribute is enabled on the created AWS loadbalancer target or not.';

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE targetgroup
DROP COLUMN IF EXISTS "use_sticky_session";
