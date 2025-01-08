-- // CB-27844 remove multi rotation framework
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS idx_multiclusterrotationresource_crn_secrettype;
DROP TABLE IF EXISTS multiclusterrotationresource;

-- //@UNDO
-- SQL to undo the change goes here.
