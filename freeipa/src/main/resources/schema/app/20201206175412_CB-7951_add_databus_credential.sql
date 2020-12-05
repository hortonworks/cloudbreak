-- // CB-7951 Store databus credentials in stack
-- Migration SQL that makes the change goes here.

alter table stack add COLUMN IF NOT EXISTS databuscredential varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

alter table stack drop COLUMN IF EXISTS databuscredential;