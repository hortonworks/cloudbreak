-- // CB-24802 Add index for securitygroup_id column in securityrule table
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_securityrule_securitygroupid
    ON securityrule (securitygroup_id);

DROP INDEX IF EXISTS       template_id_idx;

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_securityrule_securitygroupid;
