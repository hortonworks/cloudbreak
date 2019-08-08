-- // CB-2910 renaming ambariip to clustermanagerip
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster RENAME COLUMN ambariip TO clustermanagerip;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster RENAME COLUMN clustermanagerip TO ambariip;
