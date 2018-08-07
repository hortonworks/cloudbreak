-- // DPAAS-301 save username to userprofile table
-- Migration SQL that makes the change goes here.

ALTER TABLE userprofile ADD COLUMN username text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE userprofile DROP COLUMN username;
