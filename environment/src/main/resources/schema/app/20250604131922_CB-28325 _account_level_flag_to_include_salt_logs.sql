-- // CB-28325 Add account level flag to include salt logs in cloud storage
-- Migration SQL that makes the change goes here.

ALTER TABLE accounttelemetry ADD COLUMN IF NOT EXISTS enabledsensitivestoragelogs text;

-- //@UNDO
-- SQL to undo the change goes here.