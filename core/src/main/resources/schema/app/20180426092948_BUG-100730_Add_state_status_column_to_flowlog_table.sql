-- // BUG-100730 Add state status column to flowlog table
-- Migration SQL that makes the change goes here.
ALTER TABLE flowlog ADD COLUMN statestatus CHARACTER VARYING(255);


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE flowlog DROP COLUMN IF EXISTS statestatus;


