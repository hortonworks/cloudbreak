-- // CB-3175 Redbeams - CF stack owner and cb user tags contains crn
-- Migration SQL that makes the change goes here.

ALTER TABLE dbstack ADD COLUMN IF NOT EXISTS username TEXT;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE dbstack DROP COLUMN username;

