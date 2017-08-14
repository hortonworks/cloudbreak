-- // CLOUD-85571 Ability to specify default tags for a user
-- Migration SQL that makes the change goes here.


ALTER TABLE account_preferences ADD defaultTags TEXT;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE account_preferences DROP COLUMN IF EXISTS defaultTags;

