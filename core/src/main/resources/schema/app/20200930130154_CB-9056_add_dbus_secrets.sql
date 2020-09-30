-- // CB-9058 Prevent restart of clusters creating new machines user access keys in UMS
-- Migration SQL that makes the change goes here.

alter table cluster add COLUMN databuscredential varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

alter table cluster drop COLUMN databuscredential;