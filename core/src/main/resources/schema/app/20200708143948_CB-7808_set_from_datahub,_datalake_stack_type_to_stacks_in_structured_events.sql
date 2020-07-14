-- // CB-7808 set from datahub, datalake stack type to stacks in structured events
-- Migration SQL that makes the change goes here.



-- //@UNDO
-- SQL to undo the change goes here.

update structuredevent set resourcetype = 'stacks' where resourcetype = 'datahub' OR resourcetype = 'datalake';