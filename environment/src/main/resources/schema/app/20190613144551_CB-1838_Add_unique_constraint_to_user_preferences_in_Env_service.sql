-- // CB-1838 Add unique constraint to user preferences in Env service
-- Migration SQL that makes the change goes here.
ALTER TABLE user_preferences ADD CONSTRAINT uk_user_crn UNIQUE (user_crn);


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE user_preferences DROP CONSTRAINT IF EXISTS uk_user_crn;
