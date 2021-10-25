-- // CB-1859 set crn to stack
-- Migration SQL that makes the change goes here.
ALTER TABLE clustertemplate ADD CONSTRAINT clustertemplate_crn_uq UNIQUE (resourceCrn);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE clustertemplate DROP constraint clustertemplate_crn_uq;
