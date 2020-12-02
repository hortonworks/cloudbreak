-- // CB-9182 InvertingProxy Agent Crn initialization
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS ccmv2configs TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS ccmv2configs;


