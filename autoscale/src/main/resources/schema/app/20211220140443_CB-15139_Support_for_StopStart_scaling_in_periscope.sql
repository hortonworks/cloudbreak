-- // CB-15139 Support for StopStart scaling in periscope
-- Migration SQL that makes the change goes here.
ALTER TABLE "cluster" ADD COLUMN IF NOT EXISTS "stop_start_enabled" boolean DEFAULT false;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE "cluster" DROP COLUMN IF EXISTS "stop_start_enabled";

