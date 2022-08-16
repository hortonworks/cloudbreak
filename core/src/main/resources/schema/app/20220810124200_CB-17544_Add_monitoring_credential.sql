-- // CB-17544 Add monitoring credential
-- Migration SQL that makes the change goes here.

alter table cluster add COLUMN monitoringcredential TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

alter table cluster drop COLUMN monitoringcredential;
